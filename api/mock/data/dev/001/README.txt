
This mock data directory contains data for unit tests and/or dev purposes.

Velocity template variables are defined with date variables for each of the following:

    year_previous
    year_current
    year_next

FALL and SPRING terms are defined for each of the above three years with a prefix label of:
    term_fall
    term_spring
    term_summer

Finally, the following variables can be appended to a constructed year and term from above:
    _start
    _end

    _preReg_start
    _preReg_end

    _reg_start
    _reg_end

    _addDeadline
    _withdrawlDeadline
    _censusDate
    _dropDeadline

Lastly, these non-date specific values are also available:
    misCode
    districtMisCode

See Terms.vm for example usage of above.

