package api.colleague.collection

import groovy.transform.CompileStatic

/**
 * Iterable for a tuple.
 * <p>
 * In the context of Colleague, this is useful for traversing associations.
 */
@CompileStatic
class TupleIterable implements Iterable<Tuple> {

    private final Object[][] values

    TupleIterable(Object[] ... values) {
        this.values = values
    }

    @Override
    TupleIterator iterator() {
        return new TupleIterator(values)
    }
}
