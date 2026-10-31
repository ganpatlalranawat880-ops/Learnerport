package com.example.data.model

object CountryStateData {
  val countries = listOf(
    "India",
    "United States",
    "United Kingdom",
    "Canada",
    "Australia",
    "United Arab Emirates",
    "Germany",
    "France",
    "Singapore",
    "Saudi Arabia",
    "Nepal",
    "Bangladesh",
    "Japan",
    "Other Country"
  )

  private val countryStateMap = mapOf(
    "India" to listOf(
      "Andhra Pradesh",
      "Arunachal Pradesh",
      "Assam",
      "Bihar",
      "Chhattisgarh",
      "Goa",
      "Gujarat",
      "Haryana",
      "Himachal Pradesh",
      "Jharkhand",
      "Karnataka",
      "Kerala",
      "Madhya Pradesh",
      "Maharashtra",
      "Manipur",
      "Meghalaya",
      "Mizoram",
      "Nagaland",
      "Odisha",
      "Punjab",
      "Rajasthan",
      "Sikkim",
      "Tamil Nadu",
      "Telangana",
      "Tripura",
      "Uttar Pradesh",
      "Uttarakhand",
      "West Bengal",
      "Andaman and Nicobar Islands",
      "Chandigarh",
      "Dadra and Nagar Haveli and Daman and Diu",
      "Delhi",
      "Jammu and Kashmir",
      "Ladakh",
      "Lakshadweep",
      "Puducherry"
    ),
    "United States" to listOf(
      "Alabama", "Alaska", "Arizona", "Arkansas", "California", "Colorado",
      "Connecticut", "Delaware", "Florida", "Georgia", "Hawaii", "Idaho",
      "Illinois", "Indiana", "Iowa", "Kansas", "Kentucky", "Louisiana",
      "Maine", "Maryland", "Massachusetts", "Michigan", "Minnesota",
      "Mississippi", "Missouri", "Montana", "Nebraska", "Nevada",
      "New Hampshire", "New Jersey", "New Mexico", "New York",
      "North Carolina", "North Dakota", "Ohio", "Oklahoma", "Oregon",
      "Pennsylvania", "Rhode Island", "South Carolina", "South Dakota",
      "Tennessee", "Texas", "Utah", "Vermont", "Virginia", "Washington",
      "West Virginia", "Wisconsin", "Wyoming", "Washington D.C."
    ),
    "United Kingdom" to listOf(
      "England",
      "Scotland",
      "Wales",
      "Northern Ireland"
    ),
    "Canada" to listOf(
      "Alberta",
      "British Columbia",
      "Manitoba",
      "New Brunswick",
      "Newfoundland and Labrador",
      "Nova Scotia",
      "Ontario",
      "Prince Edward Island",
      "Quebec",
      "Saskatchewan",
      "Northwest Territories",
      "Nunavut",
      "Yukon"
    ),
    "Australia" to listOf(
      "New South Wales",
      "Victoria",
      "Queensland",
      "Western Australia",
      "South Australia",
      "Tasmania",
      "Australian Capital Territory",
      "Northern Territory"
    ),
    "United Arab Emirates" to listOf(
      "Abu Dhabi",
      "Dubai",
      "Sharjah",
      "Ajman",
      "Umm Al Quwain",
      "Ras Al Khaimah",
      "Fujairah"
    ),
    "Germany" to listOf(
      "Baden-Württemberg",
      "Bavaria",
      "Berlin",
      "Brandenburg",
      "Bremen",
      "Hamburg",
      "Hesse",
      "Lower Saxony",
      "Mecklenburg-Vorpommern",
      "North Rhine-Westphalia",
      "Rhineland-Palatinate",
      "Saarland",
      "Saxony",
      "Saxony-Anhalt",
      "Schleswig-Holstein",
      "Thuringia"
    ),
    "France" to listOf(
      "Auvergne-Rhône-Alpes",
      "Bourgogne-Franche-Comté",
      "Brittany",
      "Centre-Val de Loire",
      "Corsica",
      "Grand Est",
      "Hauts-de-France",
      "Île-de-France",
      "Normandy",
      "Nouvelle-Aquitaine",
      "Occitanie",
      "Pays de la Loire",
      "Provence-Alpes-Côte d'Azur"
    ),
    "Singapore" to listOf(
      "Central Region",
      "East Region",
      "North Region",
      "North-East Region",
      "West Region"
    ),
    "Saudi Arabia" to listOf(
      "Riyadh",
      "Makkah",
      "Eastern Province",
      "Madinah",
      "Asir",
      "Tabuk",
      "Qassim",
      "Hail",
      "Jazan",
      "Najran",
      "Al Bahah",
      "Al Jawf",
      "Northern Borders"
    ),
    "Nepal" to listOf(
      "Bagmati",
      "Gandaki",
      "Karnali",
      "Koshi",
      "Lumbini",
      "Madhesh",
      "Sudurpashchim"
    ),
    "Bangladesh" to listOf(
      "Dhaka",
      "Chittagong",
      "Rajshahi",
      "Khulna",
      "Barisal",
      "Sylhet",
      "Rangpur",
      "Mymensingh"
    ),
    "Japan" to listOf(
      "Tokyo",
      "Osaka",
      "Kyoto",
      "Hokkaido",
      "Aichi",
      "Fukuoka",
      "Kanagawa",
      "Hyogo"
    ),
    "Other Country" to listOf(
      "General / National",
      "Northern Province",
      "Southern Province",
      "Eastern Province",
      "Western Province",
      "Central Capital Territory"
    )
  )

  fun getStates(country: String): List<String> {
    return countryStateMap[country] ?: listOf("General / State Capital")
  }
}
