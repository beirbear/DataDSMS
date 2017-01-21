package DataDSMS

/**
  * Created by beir on 1/9/17.
  */

object Definitions {

  def getMetaDataPath = "metadata.txt"
  def getStoragePath = sys.env("DSMS_S_PATH")
  def getArchivePath = sys.env("DSMS_A_PATH")
  def getSecureKeyPath = sys.env("DSMS_SEC_KEY")
  def getSettingKeyPath = sys.env("DSMS_SET_KEY")
  def getIpAddr = sys.env("DSMS_IP_ADDR")

/*
  def getStoragePath = "/home/beir/Desktop/storage/settrade/"
  def getArchivePath = "/home/beir/Desktop/archive/"
  def getSecureKeyPath = "/home/beir/Desktop/dailyKey.key"
  def getSettingKeyPath = "/home/beir/Desktop/settingKey.key"
  def getIpAddr = ""
  */
}
