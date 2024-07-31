package com.ccctc.adaptor.model.mock;

import com.ccctc.adaptor.config.JacksonConfig;
import com.ccctc.adaptor.exception.EntityConflictException;
import com.ccctc.adaptor.exception.EntityNotFoundException;
import com.ccctc.adaptor.exception.InternalServerException;
import com.ccctc.adaptor.exception.InvalidRequestException;
import com.ccctc.adaptor.model.PrimaryKey;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.ObjectUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.tools.generic.DateTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import java.beans.PropertyDescriptor;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Base class for mock data with in-memory database functionality
 */
abstract class BaseMockDataDB<T> {

    protected final Logger logger;

    private final Class<T> clazz;
    private final List<String> keyProperties;
    private final Map<String, PropertyDescriptor> propertyDescriptorMap;

    @Autowired(required = false)
    private ObjectMapper mapper;

    @Value("${mock.data.dir}")
    private String dataDir;

    @Value("${misCode}")
    private String misCode;
    private String districtMisCode;

    private final String fileName;

    protected Map<List<Object>, T> database = new ConcurrentHashMap<>();
    private ConcurrentLinkedQueue<Hook<T>> hooks = new ConcurrentLinkedQueue<>();

    /**
     * Set up mock DB by file name and class
     *
     * @param fileName File name of data to load
     * @param clazz    Class type of a record. Must be annotated with PrimaryKey.
     */
    protected BaseMockDataDB(String fileName, Class<T> clazz) throws Exception {
        this.logger = LoggerFactory.getLogger(this.getClass());
        this.fileName = fileName;
        this.clazz = clazz;

        logger.debug("Loading " + this.getClass().getSimpleName() + " mock database");

        // Get property descriptors for the class
        propertyDescriptorMap = new HashMap<>();
        for (PropertyDescriptor pd : BeanUtils.getPropertyDescriptors(clazz)) {
            propertyDescriptorMap.put(pd.getName(), pd);
        }

        // Get primary key information
        PrimaryKey[] c = clazz.getAnnotationsByType(PrimaryKey.class);

        if (c.length > 0) {
            keyProperties = Arrays.asList(c[0].value());
        } else {
            throw new IllegalArgumentException("Unable to create mock DB for class " + clazz.getSimpleName() + " PrimaryKey annotation is missing or invalid");
        }
    }

    /**
     * Load database after dependency injection is complete
     */
    @PostConstruct
    protected void init() {
        try {
            // cleanse misCode, derive district
            misCode = misCode != null && misCode.length() == 3 ? misCode : "001";
            districtMisCode = misCode.substring(0, 2) + "0";

            // register hooks
            this.registerHooks();

            // assign jackson mapper if not injected
            if (mapper == null)
                mapper = new JacksonConfig().jacksonObjectMapper();

            // load data
            if (dataDir != null && fileName != null)
                this.loadData();

        } catch (Exception e) {
            logger.error("Failed to init {}: {}", this.getClass().getSimpleName(), e.getMessage(), e);
        }
    }

    protected void setDataDir(String dataDir) {
        this.dataDir = dataDir;
    }

    protected void setMisCode(String misCode) {
        this.misCode = misCode;
        if (misCode != null && misCode.length() == 3)
            this.districtMisCode = misCode.substring(0, 2) + "0";
    }

    /**
     * Get all records from the database
     *
     * @return List of records
     */
    public List<T> getAll() {
        logger.trace("getAll");

        List<T> records = deepCopyList(database.values());

        for (T record : records)
            runHooks(EventType.afterGet, keyToMap(getPrimaryKey(record)), record, null, null, null);

        return records;
    }

    /**
     * Get all records from the database, sorting by primary key
     *
     * @return List of records
     */
    public List<T> getAllSorted() {
        List<T> results = getAll();

        sort(results);

        return results;
    }

    /**
     * Get a record from the database by its primary key
     *
     * @param key Key to record
     */
    protected T get(List<Object> key) {
        logger.trace("get {} {}", clazz.getSimpleName(), key);

        runHooks(EventType.checkParameters, keyToMap(key), null, null, null, null);

        T record = database.get(key);
        if (record == null)
            throw new EntityNotFoundException(clazz.getSimpleName() + " record not found");

        T copy = deepCopy(record);
        runHooks(EventType.afterGet, keyToMap(key), copy, null, null, null);
        return copy;
    }

    /**
     * Get a record from the database by its primary key, bypassing hooks
     *
     * @param key Key to record
     */
    protected T getNoHooks(List<Object> key) {
        logger.trace("get no hooks {} {}", clazz.getSimpleName(), key);

        T record = database.get(key);
        if (record == null)
            throw new EntityNotFoundException(clazz.getSimpleName() + " record not found");

        return deepCopy(record);
    }

    /**
     * Get a list of records from the database by searching by a map of field/value pairs
     *
     * @param searchMap Values to search for
     */
    public List<T> find(Map<String, Object> searchMap) {
        logger.trace("find {} {}", clazz.getSimpleName(), searchMap.toString());

        runHooks(EventType.checkParameters, searchMap, null, null, null, null);

        List<T> results = new ArrayList<>();

        for (T record : database.values()) {
            boolean match = true;
            for (Map.Entry<String, Object> e : searchMap.entrySet()) {
                Object a = e.getValue();
                Object b = getPropertyValue(e.getKey(), record);

                // if class types do not match, convert to strings
                // convert date values to a numeric string for comparison
                if (a != null && b != null && a.getClass() != b.getClass()) {
                    a = a instanceof Date ? Long.toString(((Date) a).getTime()) : a.toString();
                    b = b instanceof Date ? Long.toString(((Date) b).getTime()) : b.toString();
                }

                if (!ObjectUtils.equals(a, b)) {
                    match = false;
                    break;
                }
            }

            if (match) results.add(record);
        }

        List<T> records = deepCopyList(results);

        for (T record : records)
            runHooks(EventType.afterGet, keyToMap(getPrimaryKey(record)), record, null, null, null);

        return records;
    }

    /**
     * Get a list of records from the database by searching by a map of field/value pairs. Sorted by primary key.
     *
     * @param searchMap Values to search for
     */
    public List<T> findSorted(Map<String, Object> searchMap) {
        List<T> results = find(searchMap);

        sort(results);

        return results;
    }

    /**
     * Sort a list of data by primary key
     *
     * @param data Records to sort
     */
    protected void sort(List<T> data) {
        if (data.size() > 1) {
            logger.trace("Sorting {} record(s)", data.size());

            data.sort((a, b) -> {
                for (String k : keyProperties) {
                    Object p1 = getPropertyValue(k, a);
                    Object p2 = getPropertyValue(k, b);

                    logger.trace("Sort - comparing {} to {}", p1, p2);

                    if (ObjectUtils.equals(p1, p2)) continue;

                    if (p1 instanceof Comparable)
                        return ((Comparable) p1).compareTo(p2);

                    return p1.toString().compareTo(p2.toString());
                }

                return 0;
            });
        }
    }

    /**
     * Add a record to the database
     *
     * @param record Record to add
     * @return Record
     */
    public T add(T record) {
        logger.trace("add {} {}", clazz.getSimpleName(), record.toString());

        // make a copy so we leave the original record untouched
        T recordCopy = deepCopy(record);

        List<Object> key = getPrimaryKey(recordCopy);

        runHooks(EventType.checkParameters, keyToMap(key), null, null, null, null);

        // ensure that all key fields are populated
        int x = 0;
        for (Object k : key) {
            if (k == null)
                throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "Required field " + keyProperties.get(x) + " is missing");

            x++;
        }

        if (database.containsKey(key))
            throw new EntityConflictException(clazz.getSimpleName() + " record already exists");

        runHooks(EventType.beforeAdd, keyToMap(key), null, recordCopy, null, null);
        database.put(key, recordCopy);
        try {
            runHooks(EventType.afterAdd, keyToMap(key), null, recordCopy, null, null);
        } catch (Exception e) {
            // roll back
            database.remove(key);
            throw e;
        }

        return get(key);
    }

    /**
     * Replace a record in the database
     *
     * @param key         Record key
     * @param replacement Replacement record
     * @return Updated record
     */
    protected T update(List<Object> key, T replacement) {
        logger.trace("update {} {} {}", clazz.getSimpleName(), key.toString(), replacement.toString());

        T original = get(key);

        // make a copy so we leave the original record untouched
        T replacementCopy = deepCopy(replacement);

        // ensure key values have not changed, assign where they are missing in replacement record
        for (String p : keyProperties) {
            Object a = getPropertyValue(p, replacementCopy);
            Object b = getPropertyValue(p, original);
            if (a == null) {
                // copy key value
                PropertyDescriptor pd = propertyDescriptorMap.get(p);
                try {
                    pd.getWriteMethod().invoke(replacementCopy, b);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new InternalServerException("Exception using setter of " + pd.getName() + ": " + e.getMessage());
                }
            } else if (!a.equals(b)) {
                throw new InvalidRequestException(InvalidRequestException.Errors.invalidRequest, "Update cannot change value of " + p);
            }
        }

        runHooks(EventType.beforeUpdate, keyToMap(key), original, replacementCopy, null, null);
        database.put(key, replacementCopy);
        try {
            runHooks(EventType.afterUpdate, keyToMap(key), original, replacementCopy, null, null);
        } catch (Exception e) {
            // roll back
            database.put(key, original);
            throw e;
        }

        return get(key);
    }

    /**
     * Patch a record in the database
     *
     * @param key    Record key
     * @param source Map of elements to update
     * @return Updated record
     */
    public T patch(List<Object> key, Map source) {
        T original = get(key);

        // make copy of original record
        T replacement = deepCopy(original);

        // convert map to a record for ease of copying values (jackson handles some potential data typing issues)
        T mapped = mapper.convertValue(source, clazz);

        // update record for fields specified in the source map
        for (PropertyDescriptor p : propertyDescriptorMap.values()) {
            try {
                if (source.containsKey(p.getName())) {
                    Object v = p.getReadMethod().invoke(mapped);
                    p.getWriteMethod().invoke(replacement, v);
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new InternalServerException("Exception using getter or setter of " + p.getName() + ": " + e.getMessage());
            }
        }

        // we're now ready to perform the update
        return update(key, replacement);
    }

    /**
     * Delete a record from the database
     *
     * @param key     Record key
     * @param cascade Cascade delete?
     * @return Record that was deleted
     */
    protected T delete(List<Object> key, boolean cascade) {
        logger.trace("delete {} {} {}", clazz.getSimpleName(), key, cascade);

        T record = get(key);

        // check referential integrity before deleting so that we don't accidentally
        // delete some records while leaving others intact
        if (!cascade) {
            runHooks(EventType.beforeDelete, keyToMap(key), record, null, false, true);
        }

        runHooks(EventType.beforeDelete, keyToMap(key), record, null, cascade, false);
        database.remove(key);
        try {
            runHooks(EventType.afterDelete, keyToMap(key), record, null, cascade, false);
        } catch (Exception e) {
            // roll back
            database.put(key, record);
            throw e;
        }

        return record;
    }

    /**
     * Touch a record. This will trigger an updateAfter event without changing the record.
     *
     * @param key Record key
     * @return Record
     */
    protected T touch(List<Object> key) {
        T record = get(key);
        runHooks(EventType.afterUpdate, keyToMap(key), record, record, null, null);
        return record;
    }

    /**
     * Delete many records with optional cascade. Set test to true to throw an exception if there are records present.
     * This should be called from beforeDelete operations in parent records to perform cascading deletes.
     *
     * @param criteria Criteria
     * @param cascade  Cascade delete?
     * @param test     Test whether the process will delete records? (will throw exception if records exist)
     */
    protected void deleteMany(Map<String, Object> criteria, boolean cascade, boolean test) {
        List<T> records = find(criteria);

        if (records.size() > 0) {
            if (!test) {
                for (T record : records) {
                    delete(getPrimaryKey(record), cascade);
                }
            } else {
                throw new InvalidRequestException(null, "Unable to perform delete - " + clazz.getSimpleName()
                        + " record(s) exist. Enable cascade to force delete.");
            }
        }
    }

    /**
     * Delete many records with optional cascade. Set test to true to throw an exception if there are records present.
     * This should be called from beforeDelete operations in parent records to perform cascading deletes.
     *
     * @param keys    Record keys
     * @param cascade Cascade delete?
     * @param test    Test whether the process will delete records? (will throw exception if records exist)
     */
    protected void deleteMany(List<List<Object>> keys, boolean cascade, boolean test) {
        if (keys.size() > 0) {
            if (!test) {
                for (List<Object> key : keys) {
                    delete(key, cascade);
                }
            } else {
                throw new InvalidRequestException(null, "Unable to perform delete - " + clazz.getSimpleName()
                        + " record(s) exist. Enable cascade to force delete.");
            }
        }
    }

    /**
     * Delete all records from the database
     *
     * @param cascade Cascade delete?
     */
    public void deleteAll(boolean cascade) {
        if (database.size() > 0) {
            List<List<Object>> keys = new ArrayList<>(database.keySet());
            for (List<Object> key : keys) {
                delete(key, cascade);
            }
        }
    }

    /**
     * Method to be implemented in subclass to register hooks. Registration occurs before data is loaded and after
     * dependency injection is complete.
     */
    abstract void registerHooks();

    /**
     * Add a hook to the database
     *
     * @param eventType Event Type
     * @param consumer  Consumer
     */
    void addHook(EventType eventType, Consumer<EventDetail<T>> consumer) {
        hooks.add(new Hook<>(eventType, consumer));
    }

    /**
     * Run hooks for an event
     *
     * @param eventType     Event Type
     * @param parameters    Event parameters
     * @param original      Original record (when updating or deleting)
     * @param replacement   Replacement record (when adding or updating)
     * @param cascadeDelete Cascade delete?
     * @param testDelete    Test delete?
     */
    private void runHooks(EventType eventType, Map<String, Object> parameters, T original, T replacement, Boolean cascadeDelete, Boolean testDelete) {
        runHooks(new EventDetail<>(eventType, parameters, original, replacement, cascadeDelete, testDelete));
    }

    /**
     * Run hooks for an event
     *
     * @param eventDetail Event Detail
     */
    private void runHooks(EventDetail<T> eventDetail) {
        for (Hook<T> hook : hooks) {
            if (hook.getEventType() == eventDetail.getEventType()) {
                hook.getConsumer().accept(eventDetail);
            }
        }
    }

    /**
     * Get the value of a property from a record
     *
     * @param propertyName Name of property to get
     * @param record       Record
     * @return Value
     */
    Object getPropertyValue(String propertyName, T record) {
        PropertyDescriptor p = propertyDescriptorMap.get(propertyName);
        if (p == null)
            throw new InternalServerException("Property not found: " + propertyName);

        Method m = p.getReadMethod();

        if (m == null)
            throw new InternalServerException("Getter not found for: " + propertyName);

        try {
            return m.invoke(record);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new InternalServerException("Exception invoking " + m.getName() + ": " + e.getMessage());
        }
    }


    /**
     * Get the primary key of a record
     *
     * @param record Record
     * @return Primary key as a list of values
     */
    List<Object> getPrimaryKey(T record) {
        List<Object> result = new ArrayList<>();

        for (String key : keyProperties) {
            Object o = getPropertyValue(key, record);
            result.add(o);
        }

        return result;
    }

    /**
     * Convert a primary key list to a primary key map
     *
     * @param key Primary key
     * @return Map of primary key
     */
    Map<String, Object> keyToMap(List<Object> key) {
        Map<String, Object> result = new HashMap<>();

        int x = 0;
        for (String k : keyProperties) {
            if (key.size() > x)
                result.put(k, key.get(x++));
        }

        return result;
    }

    /**
     * Perform a deep copy of a record
     *
     * @param record Record to copy
     * @return Copy of record
     */
    T deepCopy(T record) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(record);
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais);
            return (T) ois.readObject();
        } catch (Exception e) {
            throw new InternalServerException("Error copying " + clazz.getSimpleName() + " record: " + e.toString());
        }
    }

    /**
     * Perform a deep copy of a list of records
     *
     * @param records Records to copy
     * @return Copy of record
     */
    List<T> deepCopyList(Iterable<T> records) {
        List<T> results = new ArrayList<>();
        for (T record : records) {
            results.add(deepCopy(record));
        }

        return results;
    }

    /**
     * Loads contents of filename as a Velocity template and returns the merged contents of the Velocity
     * context and template file
     */
    public void loadData() {
        logger.info("Loading data template {} from {}, base = {}", fileName, dataDir + "/" + misCode, new File(".").getAbsolutePath());

        Properties props = new Properties();
        props.put("file.resource.loader.path", Paths.get(dataDir + "/" + misCode).toString());
        Velocity.init(props);

        Template t = Velocity.getTemplate(fileName);

        VelocityContext ctx = new VelocityContext();
        ctx.put("misCode", misCode);
        ctx.put("districtMisCode", districtMisCode);
        // allow Date/Calendar manipulations inside template
        ctx.put("dateTool", new DateTool());

        /**
         * Handle last year, this year and next year for Fall Spring Summer and Winter semesters
         */


        if (misCode.equals("001")) {
            Calendar cal = Calendar.getInstance();
            int thisYear = cal.get(Calendar.YEAR);
            // If we're past last day of FALL term, rollover to next year
            Calendar fallStart = makeStartTerm(thisYear, Calendar.AUGUST, 15);
            Calendar fallEnd = makeEndTerm(fallStart);

            Calendar springStart = makeStartTerm(thisYear, Calendar.JANUARY, 8);
            Calendar springEnd = makeEndTerm(springStart);

            Calendar summerStart = makeStartTerm(thisYear, Calendar.JUNE, 30);
            Calendar summerEnd = makeEndTerm(summerStart);

            long now = System.currentTimeMillis();
            if (now > fallEnd.getTimeInMillis()) {
                thisYear++;
            }
            makeTerm001(ctx, thisYear);
            
        } else {
            Calendar cal = Calendar.getInstance();
            int thisYear = cal.get(Calendar.YEAR);
            // If we're past last day of FALL term, rollover to next year
            Calendar fallStart = makeStartTerm(thisYear, Calendar.AUGUST, 15);
            Calendar fallEnd = makeEndTerm(fallStart);

            Calendar springStart = makeStartTerm(thisYear, Calendar.APRIL, 9);
            Calendar springEnd = makeEndTerm(springStart);

            Calendar summerStart = makeStartTerm(thisYear, Calendar.JUNE, 30);
            Calendar summerEnd = makeEndTerm(summerStart);

            Calendar winterStart = makeStartTerm(thisYear, Calendar.JANUARY, 8);
            Calendar winterEnd = makeEndTerm(winterStart);
            long now = System.currentTimeMillis();
            if (now > fallEnd.getTimeInMillis()) {
                thisYear++;
            }
            makeTerm002(ctx, thisYear);

        }


        // now render the template into a StringWriter
        StringWriter writer = new StringWriter();
        t.merge(ctx, writer);

        // map JSON into object
        List<T> newDatabase;
        try {
            newDatabase = mapper.readValue(writer.toString(), mapper.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (IOException e) {
            throw new InternalServerException("IO Exception loading file " + fileName + "; " + e.getMessage());
        }

        // add / update database with data from the file
        Set<List<Object>> newKeys = new HashSet<>();
        for (T a : newDatabase) {
            try {
                List<Object> key = getPrimaryKey(a);

                if (database.containsKey(key)) {
                    update(key, a);
                } else {
                    add(a);
                }

                newKeys.add(key);
            } catch (Exception e) {
                // log any errors adding/updating data
                logger.error("Error adding/updating record from {} : {}", fileName, e.toString(), e);
            }
        }

        // delete old records not in file, cascading the delete
        List<List<Object>> keysToDelete = new ArrayList<>();
        keysToDelete.addAll(database.keySet());
        keysToDelete.removeAll(newKeys);
        for (List<Object> key : keysToDelete) {
            try {
                delete(key, true);
            } catch (Exception e) {
                // log any errors adding/updating data
                logger.error("Error delete record from {} : {}", fileName, e.toString());
            }
        }

    }

    /**
     * Returns a Calendar for the start of passed term info
     *
     * @param year
     * @param startMonth
     * @param startDay
     * @return
     */
    private Calendar makeStartTerm(int year, int startMonth, int startDay) {
        Calendar start = Calendar.getInstance();
        start.set(Calendar.YEAR, year);
        start.set(Calendar.MONTH, startMonth);
        start.set(Calendar.DAY_OF_MONTH, startDay);
        // start this on a Monday at beginning of day
        setToDayOfWeek(start, Calendar.MONDAY);
        setToStartOfDaY(start);
        return start;
    }

    /**
     * Returns a Calendar for the end of the term passed
     *
     * @param start
     * @return
     */
    private Calendar makeEndTerm(Calendar start) {
        // end of term : start + 18 weeks
        Calendar end = (Calendar) start.clone();
        end.add(Calendar.WEEK_OF_YEAR, 18);
        // end this on a Friday at end of day
        setToDayOfWeek(end, Calendar.FRIDAY);
        setToEndOfDaY(end);
        return end;
    }

    /**
     * Add term information into the velocity context
     *
     * @param yearLabel       Year label
     * @param termLabel       Term label
     * @param velocityContext Velocity context to update
     * @param year            Year
     * @param startMonth      Start month
     * @param startDay        Start Day
     */
    private void makeTerm(String yearLabel, String termLabel, VelocityContext velocityContext, int year, int startMonth, int startDay) {

        SimpleDateFormat yearFmt = new SimpleDateFormat("yyyy");

        String yearTermLabel = yearLabel + "_" + termLabel;

        // start of term
        Calendar start = makeStartTerm(year, startMonth, startDay);
        velocityContext.put(yearLabel, yearFmt.format(start.getTime()));
        velocityContext.put(yearTermLabel + "_start", start);

        // end of term : start + 18 weeks
        Calendar end = makeEndTerm(start);
        velocityContext.put(yearTermLabel + "_end", end);

        // pre-registration start : 6 months before term start
        Calendar preRegStart = (Calendar) start.clone();
        preRegStart.add(Calendar.MONTH, -6);
        // set this on a Monday at beginning of day
        setToDayOfWeek(preRegStart, Calendar.MONDAY);
        setToStartOfDaY(preRegStart);
        velocityContext.put(yearTermLabel + "_preReg_start", preRegStart);

        //registration  start : 3 months before term start
        Calendar regStart = (Calendar) start.clone();
        regStart.add(Calendar.MONTH, -3);
        // set this on a Monday at beginning of day
        setToDayOfWeek(regStart, Calendar.MONDAY);
        setToStartOfDaY(regStart);
        velocityContext.put(yearTermLabel + "_reg_start", regStart);

        /*
        Ending dates
         */
        // pre-registration end : day before registration start date
        Calendar preRegEnd = (Calendar) regStart.clone();
        preRegEnd.add(Calendar.DAY_OF_YEAR, -1);
        setToEndOfDaY(preRegEnd);
        velocityContext.put(yearTermLabel + "_preReg_end", preRegEnd);

        //registration  end : day before term start date
        Calendar regEnd = (Calendar) start.clone();
        regEnd.add(Calendar.DAY_OF_YEAR, -1);
        setToEndOfDaY(regEnd);
        velocityContext.put(yearTermLabel + "_reg_end", regEnd);

        // assume start/end dates are from same year
        int termDays = Math.abs(end.get(Calendar.DAY_OF_YEAR) - start.get(Calendar.DAY_OF_YEAR));

        // census date = 20%
        double censusDOY = termDays * .20;
        Calendar censusDate = (Calendar) start.clone();
        censusDate.add(Calendar.DAY_OF_YEAR, (int) censusDOY);
        velocityContext.put(yearTermLabel + "_censusDate", censusDate);

        // add deadline = 20% (== census date) - 1 day
        Calendar addDeadline = (Calendar) censusDate.clone();
        addDeadline.add(Calendar.DAY_OF_YEAR, -1);
        velocityContext.put(yearTermLabel + "_addDeadline", addDeadline);

        // withdrawal deadline = 20% - 1 day (== add deadline)
        Calendar withdrawlDeadline = (Calendar) addDeadline.clone();
        velocityContext.put(yearTermLabel + "_withdrawlDeadline", withdrawlDeadline);

        // drop deadline = 75%
        double dropDOY = termDays + .75;
        Calendar dropDeadline = (Calendar) start.clone();
        dropDeadline.add(Calendar.DAY_OF_YEAR, (int) dropDOY);
        velocityContext.put(yearTermLabel + "_dropDeadline", dropDeadline);
    }

    /**
     * Utility method to set passed Calendar time to beginning of day at 00:00:00
     *
     * @param cal
     */
    private void setToStartOfDaY(Calendar cal) {
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
    }

    /**
     * * Utility method to set passed Calendar time to beginning of day at 23:59:59
     *
     * @param cal
     */
    private void setToEndOfDaY(Calendar cal) {
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
    }

    /**
     * Utility method to set passed Calendar to closest Monday
     *
     * @param cal
     * @param dayOfWeek
     */
    private void setToDayOfWeek(Calendar cal, int dayOfWeek) {
        cal.add(Calendar.DAY_OF_WEEK, dayOfWeek - cal.get(Calendar.DAY_OF_WEEK));
    }

    public enum EventType {
        checkParameters, beforeAdd, afterAdd, beforeDelete, afterDelete, beforeUpdate, afterUpdate, afterGet
    }

    public static class Hook<T> {
        private EventType eventType;
        private Consumer<EventDetail<T>> consumer;

        public Hook(EventType eventType, Consumer<EventDetail<T>> consumer) {
            this.eventType = eventType;
            this.consumer = consumer;
        }

        public EventType getEventType() {
            return eventType;
        }

        public Consumer<EventDetail<T>> getConsumer() {
            return consumer;
        }
    }

    public static class EventDetail<T> {
        private EventType eventType;
        private Map<String, Object> parameters;
        private T original;
        private T replacement;
        private Boolean cascadeDelete;
        private Boolean testDelete;

        public EventDetail(EventType eventType, Map<String, Object> parameters, T original, T replacement, Boolean cascadeDelete, Boolean testDelete) {
            this.eventType = eventType;
            this.parameters = parameters;
            this.original = original;
            this.replacement = replacement;
            this.cascadeDelete = cascadeDelete;
            this.testDelete = testDelete;
        }

        public EventType getEventType() {
            return eventType;
        }

        public Map<String, Object> getParameters() {
            return parameters;
        }

        public T getOriginal() {
            return original;
        }

        public T getReplacement() {
            return replacement;
        }

        public Boolean getCascadeDelete() {
            return cascadeDelete;
        }

        public Boolean getTestDelete() {
            return testDelete;
        }
    }


    public void makeTerm001(VelocityContext ctx, int thisYear) {
        /*
         LAST YEAR
          */
        makeTerm("year_previous", "term_fall", ctx, thisYear - 1, Calendar.AUGUST, 15);
        makeTerm("year_previous", "term_spring", ctx, thisYear - 1, Calendar.JANUARY, 8);
        makeTerm("year_previous", "term_summer", ctx, thisYear - 1, Calendar.JUNE, 30);
        /*
         Current YEAR
          */
        makeTerm("year_current", "term_fall", ctx, thisYear, Calendar.AUGUST, 15);
        makeTerm("year_current", "term_spring", ctx, thisYear, Calendar.JANUARY, 8);
        makeTerm("year_current", "term_summer", ctx, thisYear, Calendar.JUNE, 30);
        /*
         Next YEAR
          */
        makeTerm("year_next", "term_fall", ctx, thisYear + 1, Calendar.AUGUST, 15);
        makeTerm("year_next", "term_spring", ctx, thisYear + 1, Calendar.JANUARY, 8);
        makeTerm("year_next", "term_summer", ctx, thisYear + 1, Calendar.JUNE, 30);
    }

    public void makeTerm002(VelocityContext ctx, int thisYear) {
        /*
         LAST YEAR
          */
        makeTerm("year_previous", "term_fall", ctx, thisYear - 1, Calendar.AUGUST, 15);
        makeTerm("year_previous", "term_spring", ctx, thisYear - 1, Calendar.APRIL, 9);
        makeTerm("year_previous", "term_summer", ctx, thisYear - 1, Calendar.JUNE, 30);
        makeTerm("year_previous", "term_winter", ctx, thisYear - 1, Calendar.JANUARY, 8);
        /*
         Current YEAR
          */
        makeTerm("year_current", "term_fall", ctx, thisYear, Calendar.AUGUST, 15);
        makeTerm("year_current", "term_spring", ctx, thisYear, Calendar.APRIL, 9);
        makeTerm("year_current", "term_summer", ctx, thisYear, Calendar.JUNE, 30);
        makeTerm("year_current", "term_winter", ctx, thisYear, Calendar.JANUARY, 8);
        /*
         Next YEAR
          */
        makeTerm("year_next", "term_fall", ctx, thisYear + 1, Calendar.AUGUST, 15);
        makeTerm("year_next", "term_spring", ctx, thisYear + 1, Calendar.APRIL, 9);
        makeTerm("year_next", "term_summer", ctx, thisYear + 1, Calendar.JUNE, 30);
        makeTerm("year_next", "term_winter", ctx, thisYear + 1, Calendar.JANUARY, 8);
    }
}
