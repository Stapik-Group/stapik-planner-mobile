package pl.stapik.planner.data.config

/** [passphrase] is the optional end-to-end passphrase; without it encrypted cloud data cannot be read. */
data class ApiConfig(val baseUrl: String, val apiKey: String, val passphrase: String? = null)