package com.example.model

import android.content.Context
import android.telephony.TelephonyManager
import java.util.Locale

data class CountryInfo(
    val code: String,          // e.g. "CR", "ES", "MX", "US"
    val dialCode: String,      // e.g. "+506", "+34", "+52", "+1"
    val name: String,          // e.g. "Costa Rica", "España", "México"
    val flagEmoji: String      // e.g. "🇨🇷", "🇪🇸", "🇲🇽"
)

object CountryRepository {

    val COUNTRIES: List<CountryInfo> = listOf(
        // Central & Latin America (Priority for regional users)
        CountryInfo("CR", "+506", "Costa Rica", "🇨🇷"),
        CountryInfo("MX", "+52", "México", "🇲🇽"),
        CountryInfo("CO", "+57", "Colombia", "🇨🇴"),
        CountryInfo("AR", "+54", "Argentina", "🇦🇷"),
        CountryInfo("CL", "+56", "Chile", "🇨🇱"),
        CountryInfo("PE", "+51", "Perú", "🇵🇪"),
        CountryInfo("EC", "+593", "Ecuador", "🇪🇨"),
        CountryInfo("GT", "+502", "Guatemala", "🇬🇹"),
        CountryInfo("HN", "+504", "Honduras", "🇭🇳"),
        CountryInfo("SV", "+503", "El Salvador", "🇸🇻"),
        CountryInfo("NI", "+505", "Nicaragua", "🇳🇮"),
        CountryInfo("PA", "+507", "Panamá", "🇵🇦"),
        CountryInfo("VE", "+58", "Venezuela", "🇻🇪"),
        CountryInfo("BO", "+591", "Bolivia", "🇧🇴"),
        CountryInfo("PY", "+595", "Paraguay", "🇵🇾"),
        CountryInfo("UY", "+598", "Uruguay", "🇺🇾"),
        CountryInfo("DO", "+1809", "República Dominicana", "🇩🇴"),
        CountryInfo("PR", "+1787", "Puerto Rico", "🇵🇷"),
        CountryInfo("CU", "+53", "Cuba", "🇨🇺"),
        CountryInfo("BR", "+55", "Brasil", "🇧🇷"),

        // North America & Caribbean
        CountryInfo("US", "+1", "Estados Unidos", "🇺🇸"),
        CountryInfo("CA", "+1", "Canadá", "🇨🇦"),
        CountryInfo("DO", "+1809", "República Dominicana", "🇩🇴"),
        CountryInfo("PR", "+1787", "Puerto Rico", "🇵🇷"),
        CountryInfo("CU", "+53", "Cuba", "🇨🇺"),
        CountryInfo("JM", "+1876", "Jamaica", "🇯🇲"),
        CountryInfo("HT", "+509", "Haití", "🇭🇹"),
        CountryInfo("TT", "+1868", "Trinidad y Tobago", "🇹🇹"),

        // Europe
        CountryInfo("ES", "+34", "España", "🇪🇸"),
        CountryInfo("GB", "+44", "Reino Unido", "🇬🇧"),
        CountryInfo("FR", "+33", "Francia", "🇫🇷"),
        CountryInfo("DE", "+49", "Alemania", "🇩🇪"),
        CountryInfo("IT", "+39", "Italia", "🇮🇹"),
        CountryInfo("PT", "+351", "Portugal", "🇵🇹"),
        CountryInfo("NL", "+31", "Países Bajos", "🇳🇱"),
        CountryInfo("BE", "+32", "Bélgica", "🇧🇪"),
        CountryInfo("CH", "+41", "Suiza", "🇨🇭"),
        CountryInfo("AT", "+43", "Austria", "🇦🇹"),
        CountryInfo("SE", "+46", "Suecia", "🇸🇪"),
        CountryInfo("NO", "+47", "Noruega", "🇳🇴"),
        CountryInfo("DK", "+45", "Dinamarca", "🇩🇰"),
        CountryInfo("FI", "+358", "Finlandia", "🇫🇮"),
        CountryInfo("IE", "+353", "Irlanda", "🇮🇪"),
        CountryInfo("PL", "+48", "Polonia", "🇵🇱"),
        CountryInfo("RO", "+40", "Rumanía", "🇷🇴"),
        CountryInfo("GR", "+30", "Grecia", "🇬🇷"),
        CountryInfo("CZ", "+420", "República Checa", "🇨🇿"),
        CountryInfo("HU", "+36", "Hungría", "🇭🇺"),
        CountryInfo("UA", "+380", "Ucrania", "🇺🇦"),
        CountryInfo("RU", "+7", "Rusia", "🇷🇺"),
        CountryInfo("TR", "+90", "Turquía", "🇹🇷"),
        CountryInfo("HR", "+385", "Croacia", "🇭🇷"),
        CountryInfo("RS", "+381", "Serbia", "🇷🇸"),
        CountryInfo("BG", "+359", "Bulgaria", "🇧🇬"),
        CountryInfo("SK", "+421", "Eslovaquia", "🇸🇰"),

        // Asia & Middle East
        CountryInfo("JP", "+81", "Japón", "🇯🇵"),
        CountryInfo("KR", "+82", "Corea del Sur", "🇰🇷"),
        CountryInfo("CN", "+86", "China", "🇨🇳"),
        CountryInfo("IN", "+91", "India", "🇮🇳"),
        CountryInfo("ID", "+62", "Indonesia", "🇮🇩"),
        CountryInfo("PH", "+63", "Filipinas", "🇵🇭"),
        CountryInfo("TH", "+66", "Tailandia", "🇹🇭"),
        CountryInfo("VN", "+84", "Vietnam", "🇻🇪"),
        CountryInfo("MY", "+60", "Malasia", "🇲🇾"),
        CountryInfo("SG", "+65", "Singapur", "🇸🇬"),
        CountryInfo("AE", "+971", "Emiratos Árabes Unidos", "🇦🇪"),
        CountryInfo("SA", "+966", "Arabia Saudita", "🇸🇦"),
        CountryInfo("IL", "+972", "Israel", "🇮🇱"),
        CountryInfo("TR", "+90", "Turquía", "🇹🇷"),

        // Oceania
        CountryInfo("AU", "+61", "Australia", "🇦🇺"),
        CountryInfo("NZ", "+64", "Nueva Zelanda", "🇳🇿"),

        // Africa
        CountryInfo("EG", "+20", "Egipto", "🇪🇬"),
        CountryInfo("MA", "+212", "Marruecos", "🇲🇦"),
        CountryInfo("ZA", "+27", "Sudáfrica", "🇿🇦"),
        CountryInfo("NG", "+234", "Nigeria", "🇳🇬"),
        CountryInfo("KE", "+254", "Kenia", "🇰🇪")
    )

    /**
     * Intelligently detects the user's country from SIM, Telephony, or Device Locale.
     * Defaults to Costa Rica (+506) if region matches CR or if no SIM is detected,
     * but adapts to device locale.
     */
    fun detectDeviceCountry(context: Context?): CountryInfo {
        var isoCode: String? = null

        if (context != null) {
            try {
                val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                isoCode = tm?.simCountryIso?.takeIf { it.isNotBlank() }
                    ?: tm?.networkCountryIso?.takeIf { it.isNotBlank() }
            } catch (e: Exception) {
                // Permission or telephony error
            }
        }

        if (isoCode.isNullOrBlank()) {
            isoCode = Locale.getDefault().country
        }

        if (!isoCode.isNullOrBlank()) {
            val found = COUNTRIES.firstOrNull { it.code.equals(isoCode, ignoreCase = true) }
            if (found != null) {
                return found
            }
        }

        // Return Costa Rica as default requested by user, or Spain if not found
        return COUNTRIES.firstOrNull { it.code == "CR" }
            ?: CountryInfo("CR", "+506", "Costa Rica", "🇨🇷")
    }

    fun searchCountries(query: String): List<CountryInfo> {
        val cleanQuery = query.trim().lowercase().replace("+", "")
        if (cleanQuery.isEmpty()) return COUNTRIES

        return COUNTRIES.filter { country ->
            country.name.lowercase().contains(cleanQuery) ||
            country.dialCode.replace("+", "").contains(cleanQuery) ||
            country.code.lowercase().contains(cleanQuery)
        }
    }
}
