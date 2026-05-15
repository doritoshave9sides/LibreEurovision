package com.eurovisionfoss.app.data

data class Country(
    val id: String,
    val name: String,
    val flag: String,
    val runningOrder: Int,
    val artist: String = "TBA",
    val song: String = "TBA",
    val competing2026: Boolean = true
)

val ALL_COUNTRIES = listOf(
    // 2026 Grand Final participants
    Country("LU", "Luxembourg",      "\uD83C\uDDF1\uD83C\uDDFA", 1,  "Eva Marija",                       "Mother Nature"),
    Country("EE", "Estonia",         "\uD83C\uDDEA\uD83C\uDDEA", 2,  "Vanilla Ninja",                    "Too Epic To Be True"),
    Country("AM", "Armenia",         "\uD83C\uDDE6\uD83C\uDDF2", 3,  "Sim\u00F3n",                       "Paloma Rumba"),
    Country("HR", "Croatia",         "\uD83C\uDDED\uD83C\uDDF7", 4,  "Lelek",                            "Andromeda"),
    Country("LT", "Lithuania",       "\uD83C\uDDF1\uD83C\uDDF9", 5,  "Lion Ceccah",                      "S\u00F3lo quiero m\u00E1s"),
    Country("AT", "Austria",         "\uD83C\uDDE6\uD83C\uDDF9", 6,  "Cosm\u00F3",                       "Tanzschein"),
    Country("GE", "Georgia",         "\uD83C\uDDEC\uD83C\uDDEA", 7,  "Bzikebi",                          "On Replay"),
    Country("CZ", "Czechia",         "\uD83C\uDDE8\uD83C\uDDFF", 8,  "Daniel \u017Di\u017Dka",           "Crossroads"),
    Country("BE", "Belgium",         "\uD83C\uDDE7\uD83C\uDDEA", 9,  "Essyla",                           "Dancing on the Ice"),
    Country("NO", "Norway",          "\uD83C\uDDF3\uD83C\uDDF4", 10, "Jonas Lovv",                       "Ya ya ya"),
    Country("GR", "Greece",          "\uD83C\uDDEC\uD83C\uDDF7", 11, "Akylas",                           "Ferto"),
    Country("AL", "Albania",         "\uD83C\uDDE6\uD83C\uDDF1", 12, "Alis",                             "N\u00E2n"),
    Country("FR", "France",          "\uD83C\uDDEB\uD83C\uDDF7", 13, "Monroe",                           "Regarde !"),
    Country("CY", "Cyprus",          "\uD83C\uDDE8\uD83C\uDDFE", 14, "Antigoni",                         "Jalla"),
    Country("IT", "Italy",           "\uD83C\uDDEE\uD83C\uDDF9", 15, "Sal Da Vinci",                     "Per sempre s\u00EC"),
    Country("RS", "Serbia",          "\uD83C\uDDF7\uD83C\uDDF8", 16, "Lavina",                           "Kraj mene"),
    Country("AU", "Australia",       "\uD83C\uDDE6\uD83C\uDDFA", 17, "Delta Goodrem",                    "Eclipse"),
    Country("FI", "Finland",         "\uD83C\uDDEB\uD83C\uDDEE", 18, "Linda Lampenius & Pete Parkkonen", "Liekinheitin"),
    Country("IL", "Israel",          "\uD83C\uDDEE\uD83C\uDDF1", 19, "Noam Bettan",                      "Michelle"),
    Country("AZ", "Azerbaijan",      "\uD83C\uDDE6\uD83C\uDDFF", 20, "Jiva",                             "Just Go"),
    Country("MT", "Malta",           "\uD83C\uDDF2\uD83C\uDDF9", 21, "Aidan",                            "Bella"),
    Country("DK", "Denmark",         "\uD83C\uDDE9\uD83C\uDDF0", 22, "S\u00F8ren Torpegaard Lund",       "F\u00F8r vi g\u00E5r hjem"),
    Country("PT", "Portugal",        "\uD83C\uDDF5\uD83C\uDDF9", 23, "Bandidos do Cante",                "Rosa"),
    Country("DE", "Germany",         "\uD83C\uDDE9\uD83C\uDDEA", 24, "Sarah Engels",                     "Fire"),
    Country("SE", "Sweden",          "\uD83C\uDDF8\uD83C\uDDEA", 25, "Felicia",                          "My System"),
    Country("CH", "Switzerland",     "\uD83C\uDDE8\uD83C\uDDED", 26, "Veronica Fusaro",                  "Alice"),
    Country("UA", "Ukraine",         "\uD83C\uDDFA\uD83C\uDDE6", 27, "Lel\u00E9ka",                      "Ridnym"),
    Country("GB", "United Kingdom",  "\uD83C\uDDEC\uD83C\uDDE7", 28, "Look Mum No Computer",             "Eins, Zwei, Drei"),
    Country("BG", "Bulgaria",        "\uD83C\uDDE7\uD83C\uDDEC", 29, "Dara",                             "Bangaranga"),
    Country("LV", "Latvia",          "\uD83C\uDDF1\uD83C\uDDFB", 30, "Atvara",                           "\u0112n\u0101"),
    Country("MD", "Moldova",         "\uD83C\uDDF2\uD83C\uDDE9", 31, "Satoshi",                          "Viva, Moldova"),
    Country("ME", "Montenegro",      "\uD83C\uDDF2\uD83C\uDDEA", 32, "Tamara \u017Divkovi\u0107",        "Nova zora"),
    Country("PL", "Poland",          "\uD83C\uDDF5\uD83C\uDDF1", 33, "Alicja",                           "Pray"),
    Country("RO", "Romania",         "\uD83C\uDDF7\uD83C\uDDF4", 34, "Alexandra C\u0103pit\u0103nescu",  "Choke Me"),
    Country("SM", "San Marino",      "\uD83C\uDDF8\uD83C\uDDF2", 35, "Senhit",                           "Superstar"),

    // Boycotted 2026 — included for onboarding country picker
    Country("IS", "Iceland",         "\uD83C\uDDEE\uD83C\uDDF8", 36, "Withdrew", "Boycott 2026", competing2026 = false),
    Country("IE", "Ireland",         "\uD83C\uDDEE\uD83C\uDDEA", 37, "Withdrew", "Boycott 2026", competing2026 = false),
    Country("NL", "Netherlands",     "\uD83C\uDDF3\uD83C\uDDF1", 38, "Withdrew", "Boycott 2026", competing2026 = false),
    Country("SI", "Slovenia",        "\uD83C\uDDF8\uD83C\uDDEE", 39, "Withdrew", "Boycott 2026", competing2026 = false),
    Country("ES", "Spain",           "\uD83C\uDDEA\uD83C\uDDF8", 40, "Withdrew", "Boycott 2026", competing2026 = false),

    // Recently participated — absent in 2026
    Country("MK", "North Macedonia", "\uD83C\uDDF2\uD83C\uDDF0", 41, "N/A", "Not in 2026",    competing2026 = false)
)

/** Countries taking part in the 2026 Grand Final (scored on the voting screen). */
val COMPETING_COUNTRIES = ALL_COUNTRIES.filter { it.competing2026 }
