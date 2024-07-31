package api.colleague.collection

import spock.lang.Specification

class TupleIterableSpec extends Specification {

    def "interate"() {
        setup:
        def t = new TupleIterable(["a", "b", "c"] as String[], ["d", "e", "f"] as String[], ["g", "h", "i"] as String[])
        def i = t.iterator()

        when:
        def a = i.next()
        def b = i.next()
        def c = i.next()
        def d = i.hasNext()

        then:
        a == ["a", "d", "g"]
        b == ["b", "e", "h"]
        c == ["c", "f", "i"]
        d == false

        when:
        i.next()

        then:
        thrown NoSuchElementException
    }
}
