package com.skeebl.ultrastar

import groovy.io.FileType

import java.nio.file.Files

/**
 * User: sebastien
 * Date: 08/08/15
 * Time: 15:11
 */
class Retimer {

    public static void main(String[] args) {

        def argsMessage = """Args : path correction or --revert path
correction should be an integer (in ms)
"""
        assert args.size() == 2: argsMessage

        if (args[0] == "--revert") {
            String path = args[1]
            def dir = new File(path)
            assert dir.isDirectory(): "Directory at '$dir.absolutePath' does not exist or isn't a directory."

            revert(dir)
        } else {
            String path = args[0]
            def dir = new File(path)
            assert dir.isDirectory(): "Directory at '$path' does not exist or isn't a directory."

            assert args[1].matches(/-?\d+/): "Correction '${args[1]}' should be an integer"
            int correction = Integer.parseInt(args[1])

            rename(dir, correction)
        }
    }

    static void revert(File dir) {
        def pattern = ~/.*txt\.bkp/
        def done = 0

        dir.eachFileRecurse(FileType.FILES) { backupFile ->
            if (pattern.matcher(backupFile.name).matches()) {
                def absolutePath = backupFile.absolutePath

                def name = absolutePath.substring(0, absolutePath.length() - 4)
                File currentFile = new File(name)
                if (currentFile.exists()) {
                    currentFile.write(backupFile.text)
                    backupFile.delete()
                } else
                    backupFile.renameTo(name)
                done++
            }
        }
    }

    private static void rename(File dir, int correction) {
        def list = []
        def pattern = ~/.*txt/
        def done = 0


        dir.eachFileRecurse(FileType.FILES) { file ->
            if (pattern.matcher(file.name).matches()) {
                if (!new File(file.absolutePath + ".bkp").exists())
                    list << file
                else {
                    done++
                }
            }
        }

        println("Files .txt ignored as .bkp already there: " + done)

        for (File file : list) {
            String name = file.name

            String text = file.text
            String replacedTxt
            try {
                replacedTxt = text.replaceAll(/#GAP:(-?\d*)/) { all, time ->
                    def correctedTime = Integer.parseInt(time) + correction
                    "#GAP:$correctedTime"
                }
            } catch (NumberFormatException e) {
                println(file.absolutePath)
                println(e)
            }

            Files.copy(file.toPath(), new FileOutputStream("$file.parent/${name}.bkp"))

            if (replacedTxt)
                file.write(replacedTxt)
        }

        println("Files corrected: " + list.size())
    }
}
