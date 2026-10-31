package com.flora.app.data

/**
 * P3-05 — Privacy/Terms + consent screens (per-permission catalog).
 *
 * Permissions wizard HAVE hai; ye file har permission ka
 * reason + on/off toggle key ek jagah deti hai taaki consent
 * screen har permission par ek jaisa saaf reason dikhaye:
 *
 * - Mic 24x7: sirf wakeword sunne ke liye, hamesha-on ka saaf disclosure.
 * - Screen/camera: SIRF user ke kehne par, visible indicator ke saath.
 * - Notification read: sirf user ke kehne par reply padhne ke liye.
 * - Contacts: sirf user ke kehne par naam se call/message ke liye.
 *
 * Wording yahan final consent text hai — screen is catalog se
 * banegi, alag-alag jagah alag wording nahi hogi. Koi permission
 * chupchaap ON nahi hoti; har ek ka on/off user ke haath me hai.
 */
object ConsentCatalog {

    data class ConsentEntry(
        val key: String,
        val title: String,
        val reason: String,
        val defaultOn: Boolean
    )

    val ENTRIES: List<ConsentEntry> = listOf(
        ConsentEntry(
            key = "mic-24x7",
            title = "Mic — hamesha sunna (wakeword)",
            reason = "Flora 'Hey Flora' sunne ke liye mic on rakhta hai. " +
                "Aapki baat-cheet record karke kahin nahi bheji jati — " +
                "wakeword mile tabhi aage kaam hota hai. Kabhi bhi OFF kar sakte hain.",
            defaultOn = false
        ),
        ConsentEntry(
            key = "screen-view",
            title = "Screen dekhna",
            reason = "Sirf tab jab aap khud kahein 'screen dekho'. " +
                "Bina aapke kahe screen kabhi nahi dekhi jayegi, aur " +
                "dekhte waqt visible indicator dikhega.",
            defaultOn = false
        ),
        ConsentEntry(
            key = "camera",
            title = "Camera",
            reason = "Sirf tab jab aap khud kahein — photo/scan ke liye. " +
                "Bina aapke kahe camera kabhi nahi chalega.",
            defaultOn = false
        ),
        ConsentEntry(
            key = "notification-read",
            title = "Notifications padhna",
            reason = "Sirf tab jab aap kahein — message/notice padhkar " +
                "sunane ya reply karne ke liye. Bina kahe notifications " +
                "kabhi nahi padhe jayenge.",
            defaultOn = false
        ),
        ConsentEntry(
            key = "contacts",
            title = "Contacts",
            reason = "Sirf tab jab aap naam se call ya message karne ko kahein. " +
                "Contacts kahin upload nahi hote, phone par hi rehte hain.",
            defaultOn = false
        )
    )

    fun entryFor(key: String): ConsentEntry? = ENTRIES.firstOrNull { it.key == key }
}
