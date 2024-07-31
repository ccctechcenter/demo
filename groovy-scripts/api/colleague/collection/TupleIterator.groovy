package api.colleague.collection

import groovy.transform.CompileStatic

import static api.colleague.util.ColleagueUtils.getAt

/**
 * Iterator over a tuple.
 * <p>
 * In the context of Colleague, this is useful for traversing associations.
 */
@CompileStatic
class TupleIterator implements Iterator<Tuple> {
    private int index = 0
    private final int size
    private final int elements
    private final Object[][] values

    TupleIterator(Object[][] values) {
        this.values = values
        this.elements = values.length

        // size is based on the number of elements in the first value
        this.size = values[0].length
    }

    @Override
    boolean hasNext() {
        return index < size
    }

    @Override
    Tuple next() {
        if (index >= size)
            throw new NoSuchElementException()

        def contents = new Object[elements]
        for (int x = 0; x < elements; x++) {
            contents[x] = getAt(values[x], index)
        }

        index++

        return new Tuple(contents)
    }
}