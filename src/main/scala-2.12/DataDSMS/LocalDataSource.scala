package DataDSMS

import java.io.{File, FileNotFoundException}

import DataDSMS.Services.Logger

import scala.collection.mutable.ListBuffer

case class Record(fileName: String,
                  var filePath: String,
                  var hash: String,
                  var fileSize: Long,
                  var isArchived: Boolean,
                  var isFetched: Boolean)

object LocalDataSource {

  val metaDataFile: String = Definitions.getMetaDataPath
  val storagePath: String = Definitions.getStoragePath
  val archivePath: String = Definitions.getArchivePath
  var dataList: ListBuffer[Record] = new ListBuffer[Record]()

  def readMetaData(): Unit = {
    if (!Services.General.isFileFolderExist(metaDataFile))
      throw new FileNotFoundException("Metadata doesn't exist!")

    def getStringBoolean(input: String): Boolean = if (input == "true" ) true else false

    val localDataList: ListBuffer[Record] = new ListBuffer[Record]()

    // Read meta data file
    import scala.io.Source
    for (line <- Source.fromFile(metaDataFile).getLines) {
      val tmp = line.split('|')
      localDataList += Record(tmp(0), tmp(1), tmp(2), tmp(3).toLong, getStringBoolean(tmp(4)), getStringBoolean(tmp(5)))
    }
    dataList = localDataList
  }

  private def writeMetaFile(): Unit = {
    if (!Services.General.isFileFolderExist(storagePath))
      throw new FileNotFoundException("Storage path doesn't exist!")

    // Start write file
    import java.io._
    val file = new File(metaDataFile)
    val bw = new BufferedWriter(new FileWriter(file))
    for (line <- dataList)
      bw.write(s"${line.fileName}|${line.filePath}|${line.hash}|${line.fileSize}|${line.isArchived}|${line.isFetched}\n")
    bw.close()
  }

  def resetMetaData(): Unit = {
    if (!Services.General.isFileFolderExist(storagePath))
      throw new FileNotFoundException("Storage path doesn't exist!")

    // Get all folder in storage path
    val inputStorage = Services.General.getFoldersInFolder(storagePath)
    val rawList: ListBuffer[Record] = new ListBuffer[Record]()
    for (line <- inputStorage) {
      val fileName: String = Services.General.getFileName(line.toString)
      val filePath: String = line.toString
      val isFetch: Boolean = false
      val isArchived: Boolean = false

      rawList += Record(fileName, filePath, "", 0, isArchived, isFetch)
    }

    // Check files in the archived folder
    val inputArchive = Services.General.getFilesInFolder(archivePath)
    for (line <- inputArchive) {
      println("Processing: " + line.toString)
      val fileName: String = Services.General.getFileNameWithoutExtension(line.toString)
      val filePath: String = line.toString
      val fileSize: Long = new File(line.toString) .length
      val hashV: String = getHash(fileName)
      val isArchived: Boolean = true
      val isFetched: Boolean  = false

      rawList += Record(fileName, filePath, hashV, fileSize, isArchived, isFetched)
    }
/*
    // Update the archived
    val archivedFiles = Services.General.getFilesInFolder(Definitions.getArchivePath).map(
      e => Services.General.getFileNameWithoutExtension(e.toString))
    for (item <- rawList) {
      if (archivedFiles.contains(item.fileName.toString)) {
        item.isArchived = true
      }
    }
    println("part 4")
*/
    dataList = rawList
    writeMetaFile()
  }

  private def zipContent(): Unit = {
    for( item <- dataList ) {
      if (!item.isArchived) {
        val srcFolder = item.filePath.toString
        Services.General.zipFolder(item.filePath.toString, Definitions.getArchivePath + item.fileName)
        item.isArchived = true
        item.hash = getHash(item.fileName)
        item.filePath = Definitions.getArchivePath + item.fileName + ".zip"
        item.fileSize = new File(item.filePath) .length
        Services.General.deleteFolder(srcFolder)
        Logger.write("Source folder is deleted: " + srcFolder)
      }
    }
  }

  private def getHash(input: String): String = {

    def isDuplicatedHash(input: String): Boolean = {
      // Recusive fashion later
      dataList.exists(e => e.hash == input)
    }

    var hashStr = Services.General.getHashString(input)
    val r = scala.util.Random
    do {
        hashStr = Services.General.getHashString(input + r.nextInt.toString)
      } while (isDuplicatedHash(hashStr))
    hashStr
  }

  def updateMetaData(): Unit = {
    // Step 1: Scan for new folder in storage path
    if (!Services.General.isFileFolderExist(storagePath))
      throw new FileNotFoundException("Storage path doesn't exist!")

    // Get all folder in storage path
    val inputStorage = Services.General.getFoldersInFolder(storagePath)
    for (line <- inputStorage) {
      // Check for the existing item in meta table
      val tmp = Services.General.getFileName(line.toString)
      val res = dataList.filter(e => e.fileName == tmp)

      if (res.isEmpty){
        val fileName: String = tmp
        val filePath: String = line.toString
        val isFetch: Boolean = false
        val isArchived: Boolean = false
        dataList += Record(fileName, filePath, "", 0, isArchived, isFetch)
      }
    }

    // Step 2: Archive unzipped folder
    zipContent()
    writeMetaFile()
  }

  def isValidFile(input: String): Boolean = {
    dataList.exists(e => e.hash == input)
  }

  def getContent(input: String): Array[Byte] = {
    val line = dataList.filter(e => e.hash == input)
    if (line.isEmpty) {
      throw new Exception("Invalid hash reference!")
    }

    // Flag as downloaded.
    line.head.isFetched = true
    writeMetaFile()

    import java.nio.file.{Files, Paths}
    val byteArray = Files.readAllBytes(Paths.get(line.head.filePath))

    byteArray
  }

  def getDownloadIndex: String = {
    val tmp = dataList.filter(e => e.isArchived && !e.isFetched)
    if (tmp.isEmpty) {
      ""
    }
    else {
      val rString: StringBuffer = new StringBuffer()
      for (line <- tmp.toList)
        rString.append(line.hash + "\n")
      rString.toString
    }
  }

  def getDataListString: String = {
    val str: StringBuilder = new StringBuilder()
    for (line <- dataList)
      str.append(line.toString + "\n")
    str.toString
  }

  def removeDownloadedContent(): Unit = {
    /***
      * By definition: redundant content refer to the folder that has been archived. However, the original copy still present.
      * Then, this function will remove the content that has been available and present as unarchived content.
      */
    dataList.filter(e => e.isArchived && e.isFetched).foreach(e => Services.General.deleteFolder(e.filePath))
    updateMetaData()
  }
}
