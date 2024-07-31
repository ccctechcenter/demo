package com.ccctc.adaptor.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by jrscanlon on 6/24/15.
 */
@ApiModel
public class MeetingTime implements Serializable {

    // Colleague - csm_days_assoc.csm_monday, csm_tuesday, etc
    @ApiModelProperty(value = "Meets on Mondays.")
    private Boolean monday;
    @ApiModelProperty(value = "Meets on Tuesdays.")
    private Boolean tuesday;
    @ApiModelProperty(value = "Meets on Wednesdays.")
    private Boolean wednesday;
    @ApiModelProperty(value = "Meets on Thursdays.")
    private Boolean thursday;
    @ApiModelProperty(value = "Meets on Fridays.")
    private Boolean friday;
    @ApiModelProperty(value = "Meets on Saturdays.")
    private Boolean saturday;
    @ApiModelProperty(value = "Meets on Sundays.")
    private Boolean sunday;

    // Colleague - course_sec_meeting.csm_start_date, csm_end_date
    @ApiModelProperty(required = true, value = "Start date of the meeting pattern. May or may not be the same dates as the section.", example="8/24/2018")
    private Date start;
    @ApiModelProperty(required = true, value = "End date of the meeting pattern. May or may not be the same dates as the section.", example="12/18/2018")
    private Date end;

    // Colleague - course_sec_meeting.csm_start_time, csm_end_time
    @ApiModelProperty(value = "Start time of the meeting pattern.", example="09:00AM")
    private Date startTime;
    @ApiModelProperty(value = "End time of the meeting pattern.", example="09:50AM")
    private Date endTime;

    // Colleague - course_sec_meeting.csm_instr_method
    @ApiModelProperty(value = "Instructional method associated with the meeting pattern.", example="Lecture")
    private InstructionalMethod instructionalMethod;

    // Colleague - course_sec_meeting.csm_bldg -> buildings.bldg_location -> locations.loc_desc
    @ApiModelProperty(value = "Campus/location where the meeting pattern takes place.", example="Main Campus")
    private String campus;

    // Colleague - course_sec_meeting.csm_bldg -> buildings.bldg_desc
    @ApiModelProperty(value = "Building where the meeting pattern takes place.", example="Fine Arts")
    private String building;

    // Colleague - course_sec_meeting.csm_bldg + csm_room -> rooms.room_name
    @ApiModelProperty(value = "Room in the building where the meeting pattern takes place.", example="101")
    private String room;

    private MeetingTime(Builder builder) {
        setMonday(builder.monday);
        setTuesday(builder.tuesday);
        setWednesday(builder.wednesday);
        setThursday(builder.thursday);
        setFriday(builder.friday);
        setSaturday(builder.saturday);
        setSunday(builder.sunday);
        setStart(builder.start);
        setEnd(builder.end);
        setStartTime(builder.startTime);
        setEndTime(builder.endTime);
        setInstructionalMethod(builder.instructionalMethod);
        setCampus(builder.campus);
        setBuilding(builder.building);
        setRoom(builder.room);
    }
    /**
     * Needed for ObjectMapper to unmarshal a JSON string
     */
    private MeetingTime(){}


    public Boolean getMonday() {
        return monday;
    }

    public void setMonday(Boolean monday) {
        this.monday = monday;
    }

    public Boolean getTuesday() {
        return tuesday;
    }

    public void setTuesday(Boolean tuesday) {
        this.tuesday = tuesday;
    }

    public Boolean getWednesday() {
        return wednesday;
    }

    public void setWednesday(Boolean wednesday) {
        this.wednesday = wednesday;
    }

    public Boolean getThursday() {
        return thursday;
    }

    public void setThursday(Boolean thursday) {
        this.thursday = thursday;
    }

    public Boolean getFriday() {
        return friday;
    }

    public void setFriday(Boolean friday) {
        this.friday = friday;
    }

    public Boolean getSaturday() {
        return saturday;
    }

    public void setSaturday(Boolean saturday) {
        this.saturday = saturday;
    }

    public Boolean getSunday() {
        return sunday;
    }

    public void setSunday(Boolean sunday) {
        this.sunday = sunday;
    }

    public Date getStart() {
        return start;
    }

    public void setStart(Date start) {
        this.start = start;
    }

    public Date getEnd() {
        return end;
    }

    public void setEnd(Date end) {
        this.end = end;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public InstructionalMethod getInstructionalMethod() {
        return instructionalMethod;
    }

    public void setInstructionalMethod(InstructionalMethod instructionalMethod) {
        this.instructionalMethod = instructionalMethod;
    }

    public String getCampus() {
        return campus;
    }

    public void setCampus(String campus) {
        this.campus = campus;
    }

    public String getBuilding() {
        return building;
    }

    public void setBuilding(String building) {
        this.building = building;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }


    public static final class Builder {
        private Boolean monday;
        private Boolean tuesday;
        private Boolean wednesday;
        private Boolean thursday;
        private Boolean friday;
        private Boolean saturday;
        private Boolean sunday;
        private Date start;
        private Date end;
        private Date startTime;
        private Date endTime;
        private InstructionalMethod instructionalMethod;
        private String campus;
        private String building;
        private String room;

        public Builder() {
        }

        public Builder monday(Boolean val) {
            monday = val;
            return this;
        }

        public Builder tuesday(Boolean val) {
            tuesday = val;
            return this;
        }

        public Builder wednesday(Boolean val) {
            wednesday = val;
            return this;
        }

        public Builder thursday(Boolean val) {
            thursday = val;
            return this;
        }

        public Builder friday(Boolean val) {
            friday = val;
            return this;
        }

        public Builder saturday(Boolean val) {
            saturday = val;
            return this;
        }

        public Builder sunday(Boolean val) {
            sunday = val;
            return this;
        }

        public Builder start(Date val) {
            start = val;
            return this;
        }

        public Builder end(Date val) {
            end = val;
            return this;
        }

        public Builder startTime(Date val) {
            startTime = val;
            return this;
        }

        public Builder endTime(Date val) {
            endTime = val;
            return this;
        }

        public Builder instructionalMethod(InstructionalMethod val) {
            instructionalMethod = val;
            return this;
        }

        public Builder campus(String val) {
            campus = val;
            return this;
        }

        public Builder building(String val) {
            building = val;
            return this;
        }

        public Builder room(String val) {
            room = val;
            return this;
        }

        public MeetingTime build() {
            return new MeetingTime(this);
        }
    }

    @Override
    public String toString() {
        return "MeetingTime{" +
                "monday=" + monday +
                ", tuesday=" + tuesday +
                ", wednesday=" + wednesday +
                ", thursday=" + thursday +
                ", friday=" + friday +
                ", saturday=" + saturday +
                ", sunday=" + sunday +
                ", start=" + start +
                ", end=" + end +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", instructionalMethod=" + instructionalMethod +
                ", campus='" + campus + '\'' +
                ", building='" + building + '\'' +
                ", room='" + room + '\'' +
                '}';
    }
}
