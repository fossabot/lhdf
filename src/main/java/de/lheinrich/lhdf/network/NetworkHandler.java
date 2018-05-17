package de.lheinrich.lhdf.network;

import de.lheinrich.lhdf.security.Crypter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/*
 * Copyright (c) 2018 Lennart Heinrich
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

public abstract class NetworkHandler {

    private ObjectInputStream in;
    private ObjectOutputStream out;
    private SecretKey key;

    protected void init(ObjectInputStream in, ObjectOutputStream out, SecretKey key) {
        this.in = in;
        this.out = out;
        this.key = key;
    }

    public void write(Serializable object) {
        try {
            this.out.writeObject(Crypter.encrypt("AES", Crypter.toByteArray(object), this.key));
            this.out.flush();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public Object read() {
        try {
            return Crypter.toObject(Crypter.decrypt("AES", (byte[]) this.in.readObject(), this.key));
        } catch (IOException | ClassNotFoundException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public abstract void handle();
}
