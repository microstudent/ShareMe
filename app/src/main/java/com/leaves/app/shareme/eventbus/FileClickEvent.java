package com.leaves.app.shareme.eventbus;

import java.io.File;

/**
 * Created by Leaves on 2016/11/15.
 */

public class FileClickEvent {
    private File mFile;

    public FileClickEvent(File file) {
        mFile = file;
    }

    public File getFile() {
        return mFile;
    }
}
