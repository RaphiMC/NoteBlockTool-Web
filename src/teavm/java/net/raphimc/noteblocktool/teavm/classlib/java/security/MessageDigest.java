/*
 * This file is part of NoteBlockTool-Web - https://github.com/RaphiMC/NoteBlockTool-Web
 * Copyright (C) 2026-2026 RK_01/RaphiMC and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.raphimc.noteblocktool.teavm.classlib.java.security;

import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.MD5Digest;

import java.util.Objects;

public abstract class MessageDigest {

    public static MessageDigest getInstance(final String algorithm) throws NoSuchAlgorithmException {
        Objects.requireNonNull(algorithm, "null algorithm name");
        if (algorithm.equals("MD5")) {
            return new BouncyCastleMessageDigest(new MD5Digest());
        } else {
            throw new NoSuchAlgorithmException(algorithm + " MessageDigest not available");
        }
    }

    public void update(final byte[] input, final int offset, final int len) {
        if (input == null) {
            throw new IllegalArgumentException("No input buffer given");
        }
        if (input.length - offset < len) {
            throw new IllegalArgumentException("Input buffer too short");
        }
        this.engineUpdate(input, offset, len);
    }

    protected abstract void engineUpdate(final byte[] input, final int offset, final int len);

    public abstract byte[] digest();

    public abstract void reset();

    private static class BouncyCastleMessageDigest extends MessageDigest {

        private final Digest digest;

        private BouncyCastleMessageDigest(final Digest digest) {
            this.digest = digest;
        }

        @Override
        protected void engineUpdate(final byte[] input, final int offset, final int len) {
            Objects.checkFromIndexSize(offset, len, input.length);
            this.digest.update(input, offset, len);
        }

        @Override
        public byte[] digest() {
            final byte[] result = new byte[this.digest.getDigestSize()];
            this.digest.doFinal(result, 0);
            return result;
        }

        @Override
        public void reset() {
            this.digest.reset();
        }

    }

}
