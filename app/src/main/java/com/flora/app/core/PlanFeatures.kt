package com.flora.app.core

/**
 * Plan features client (Wave B, LFMS).
 *
 * Licence-verify server se plan ki feature list aati hai
 * (features[] — admin panel se manage hoti hai: kis plan me
 * kaun se features). Ye object aakhri mili hui list rakhta hai.
 *
 * FAIL-OPEN (FeatureFlags jaisa hi niyam): list kabhi mili hi
 * nahi, ya server ne khali list bheji, to har feature ON mana
 * jayega — server list na bheje to app ka vyavhaar bilkul
 * waisa hi rahega jaisa pehle tha. List milte hi (non-empty)
 * usme na ho to feature band mana jayega.
 *
 * Isme sirf primitives hain (koi data-package import nahi) taaki
 * core -> data ka cycle na bane — LicenceClient.verify() isko
 * har verify par feed karta hai.
 */
object PlanFeatures {

    @Volatile
    private var features: List<String>? = null

    /** Non-empty list aaye to wahi plan ki list hai. */
    fun updateFrom(list: List<String>) {
        if (list.isNotEmpty()) {
            features = list
        }
    }

    /** List na ho ya khali ho to true (fail-open). */
    fun isAllowed(key: String): Boolean {
        val f = features
        if (f.isNullOrEmpty()) return true
        return key in f
    }
}
