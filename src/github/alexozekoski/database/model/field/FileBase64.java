/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package github.alexozekoski.database.model.field;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;

/**
 *
 * @author alexo
 */
public class FileBase64 {

    private String name;

    private byte[] data;

    public FileBase64(String name, byte[] data) {
       this.setName(name);
        this.data = data;
    }

    public FileBase64(String name, String base64data) {
        this.name = name;
        setBase64(base64data);
    }

    public FileBase64(File file) throws IOException {
        if (file.exists()) {
            this.data = Files.readAllBytes(file.toPath());
        }
    }

    public FileBase64(String file) throws IOException {
        this(new File(file));
    }

    public File toFile() {
        String f = this.name;
        return new File(f);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public void updateFile() throws IOException {
        if (data == null) {
            deleteFile();
        } else {
            salveFile();
        }
    }

    public void salveFile() throws IOException {

        Files.write(toFile().toPath(), data);
    }

    public boolean deleteFile() {
        data = null;
        return toFile().delete();
    }

    public String getBase64() {
        return Base64.getEncoder().encodeToString(data);
    }

    public void setBase64(String data) {
        this.data = Base64.getDecoder().decode(data);
    }
}
