package com.myhealth.core.intelligence

import java.time.LocalDate
import java.time.MonthDay

object ZodiacHelper {

    data class ZodiacSign(
        val name: String,
        val symbol: String,
        val startMonth: Int,
        val startDay: Int,
        val endMonth: Int,
        val endDay: Int,
    )

    private val signs = listOf(
        ZodiacSign("Aries", "♈", 3, 21, 4, 19),
        ZodiacSign("Taurus", "♉", 4, 20, 5, 20),
        ZodiacSign("Gemini", "♊", 5, 21, 6, 20),
        ZodiacSign("Cancer", "♋", 6, 21, 7, 22),
        ZodiacSign("Leo", "♌", 7, 23, 8, 22),
        ZodiacSign("Virgo", "♍", 8, 23, 9, 22),
        ZodiacSign("Libra", "♎", 9, 23, 10, 22),
        ZodiacSign("Scorpio", "♏", 10, 23, 11, 21),
        ZodiacSign("Sagittarius", "♐", 11, 22, 12, 21),
        ZodiacSign("Capricorn", "♑", 12, 22, 1, 19),
        ZodiacSign("Aquarius", "♒", 1, 20, 2, 18),
        ZodiacSign("Pisces", "♓", 2, 19, 3, 20),
    )

    fun fromDate(date: LocalDate): ZodiacSign {
        val md = MonthDay.from(date)
        return signs.firstOrNull { sign ->
            val start = MonthDay.of(sign.startMonth, sign.startDay)
            val end = MonthDay.of(sign.endMonth, sign.endDay)
            if (sign.name == "Capricorn") {
                md >= start || md <= end
            } else {
                md in start..end
            }
        } ?: signs.last()
    }

    fun fromDate(month: Int, day: Int): ZodiacSign = fromDate(LocalDate.of(2000, month, day))

    fun ageFromBirthday(birthday: LocalDate): Int {
        val today = LocalDate.now()
        var age = today.year - birthday.year
        if (today.monthValue < birthday.monthValue ||
            (today.monthValue == birthday.monthValue && today.dayOfMonth < birthday.dayOfMonth)) {
            age--
        }
        return age
    }
}
