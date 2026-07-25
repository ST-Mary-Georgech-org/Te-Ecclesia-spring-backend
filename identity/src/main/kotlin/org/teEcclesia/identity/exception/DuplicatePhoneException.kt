package org.teEcclesia.identity.exception

class DuplicatePhoneException(
    message: String = "This phone number is associated with multiple accounts. Please use your National ID to reset your password."
) : RuntimeException(message)
