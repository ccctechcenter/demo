package api.colleague.transcript.util

class TranscriptUtils {

    /**
     * Translate an SX04 grade to a grade scale code. The grade itself should be translated through SX04 first, then
     * the translated code sent to this method.
     *
     * @param sx04Grade Grade value from the CAST.SX04 translation (this would be the "new code" value)
     * @return Grade Scale Code
     */
    static String getGradeCodeFromSX04(String sx04Grade) {
        switch(sx04Grade) {
            case null:
                return null
            case "A":
            case "B":
            case "C":
            case "D":
            case "F":
                return "025"
            case "IP":
                return "500"
            case "P":
            case "CR":
                return "501"
            case "NP":
            case "NC":
                return "502"
            case "W":
            case "MW":
            case "FW":
                return "509"
            case "RD":
                return "513"
            default:
                // incomplete grades start with "I", except IP which was accounted for above
                if (sx04Grade && sx04Grade.charAt(0) == "I" as char)
                    return "505"
                return null
        }
    }

    /**
     * Translate an SP02 degree value to a PESC value. Typically you would get the SP02 value by translating
     * ACAD.DEGREE or ACAD.CCD via the CAST.SP02 or CAST.SP02.A elf translation table.
     *
     * This provides a standard way to determine the type of an award as the SP02 MIS code is standardized across
     * all Community Colleges, assuming they are correctly reporting their awards.
     */
    static String translateAwardLevel(String sp02Code) {
        switch (sp02Code) {
            case null:
                return null
            // AA or AS degree
            case "A":
            case "S":
                return "2.3"
            // BA or BS degree (do not include - Community Colleges do not offer these)
            case "Y":
            case "Z":
                return null
            // various certificates
            case "T":
                return "2.6"
            case "F":
                return "2.7"
            case "E":
            case "B":
            case "L":
            case "O":
                return "2.1"
            default:
                return "2.0"
        }
    }
}
