package DataDSMS

/**
  * Created by beir on 1/9/17.
  */

object Definitions {
  val terms = System.getenv()

  def getMetaDataPath = "metadata.txt"
  def getStoragePath = terms("DSMS_S_PATH")
  def getArchivePath = terms("DSMS_A_PATH")
  def getSecureKeyPath = terms("DSMS_SEC_KEY")
  def getSettingKeyPath = terms("DSMS_SET_KEY")
  def getIpAddr = terms("DSMS_IP_ADDR")

/*
  def getStoragePath = "/home/beir/Desktop/storage/settrade/"
  def getArchivePath = "/home/beir/Desktop/archive/"
  def getSecureKeyPath = "/home/beir/Desktop/dailyKey.key"
  def getSettingKeyPath = "/home/beir/Desktop/settingKey.key"
  def getIpAddr = ""
  */
}
