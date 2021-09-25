package com.malliina.config

import com.malliina.values.ErrorMessage

class ConfigException(error: ErrorMessage) extends Exception(error.message)
