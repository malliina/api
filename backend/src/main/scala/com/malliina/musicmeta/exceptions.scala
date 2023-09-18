package com.malliina.musicmeta

class CoverNotFoundException(msg: String) extends MusicException(msg)

class MusicException(msg: String) extends Exception(msg)
