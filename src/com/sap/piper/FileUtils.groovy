package com.sap.piper

import hudson.AbortException
import java.io.File


class FileUtils implements Serializable {

    static validateDirectoryOrFileExists(script, dirOrFile) {
        if (!dirOrFile) throw new IllegalArgumentException("The parameter 'dirOrFile' can not be null or empty.")
        def exists
        try {
          exists = script.sh returnStatus: true, script: """if [[ -d $dirOrFile ]]; then
                                                              echo \"$dirOrFile exists.\"
                                                              exit 1
                                                            elif [[ -f $dirOrFile ]]; then
                                                              echo \"$dirOrFile exists.\"
                                                            else
                                                              echo \"$dirOrFile does not exist.\"
                                                              exit 0
                                                            fi
                                                         """
        } catch(AbortException e) {
          throw new AbortException("The validation of '$dirOrFile' failed. Reason: $e.message.")
        }
        if (exists == 0) throw new AbortException("'$dirOrFile' does not exist.")
    }

    static validateDirectory(script, dir) {
        if (!dir) throw new IllegalArgumentException("The parameter 'dir' can not be null or empty.")
        validateDirectoryOrFileExists(script, dir)
        def isDirectory
        try {
          isDirectory = script.sh returnStatus: true, script: """if [[ -d $dir ]]; then
                                                                   echo \"$dir is a directory.\"
                                                                   exit 1
                                                                 else
                                                                   echo \"$dir is not a directory.\"
                                                                   exit 0
                                                                 fi
                                                              """
        } catch(AbortException e) {
          throw new AbortException("The validation of '$dir' failed. Reason: $e.message.")
        }
        if (isDirectory == 0) throw new AbortException("'$dir' is not a directory.")
    }

    static validateDirectoryIsNotEmpty(script, dir) {
        if (!dir) throw new IllegalArgumentException("The parameter 'dir' can not be null or empty.")
        validateDirectory(script, dir)
        def isEmpty
        try {
          isEmpty = script.sh returnStatus: true, script: """if [ -z "\$(ls -A $dir)" ]; then
                                                               echo "$dir is empty."
                                                               exit 0
                                                             else
                                                               echo "$dir is not empty."
                                                               exit 1
                                                             fi
                                                          """
        } catch(AbortException e) {
          throw new AbortException("The validation of '$dir' failed. Reason: $e.message.")
        }
        if (isEmpty == 0) throw new AbortException("'$dir' is empty.")
    }

    static validateFile(script, filePath) {
        if (!filePath) throw new IllegalArgumentException("The parameter 'filePath' can not be null or empty.")
        validateDirectoryOrFileExists(script, filePath)
        def isFile
        try {
          isFile = script.sh returnStatus: true, script: """if [[ -f $filePath ]]; then
                                                              echo \"$filePath is a file.\"
                                                              exit 1
                                                            else
                                                              echo \"$filePath is not a file.\"
                                                              exit 0
                                                            fi
                                                         """
        } catch(AbortException e) {
          throw new AbortException("The validation of '$filePath' failed. Reason: $e.message.")
        }
        if (isFile == 0) throw new AbortException("'$filePath' is not a file.")
    }
}
