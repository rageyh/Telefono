package org.mineacademy.fo.remain.nbt;

import java.io.File;
import java.io.IOException;

public interface NBTFileHandle extends ReadWriteNBT {

    /**
     * Saves the data to the file
     *
     * @throws IOException
     */
    void save() throws IOException;

    /**
     * @return The File used to store the data
     */
    File getFile();

}