package com.saroj.lmsmobile.network

import com.saroj.lmsmobile.utils.Constants
import java.io.IOException

class NoConnectivityException(
    override val message: String = Constants.ERROR_NO_INTERNET
) : IOException(message)
