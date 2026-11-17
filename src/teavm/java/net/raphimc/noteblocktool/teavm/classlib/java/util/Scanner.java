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
package net.raphimc.noteblocktool.teavm.classlib.java.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.InputMismatchException;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Scanner {

    private static final int BUFFER_SIZE = 1024;
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\p{javaWhitespace}+");
    private static final Pattern FIND_ANY_PATTERN = Pattern.compile("(?s).*");
    private static final Pattern INTEGER_PATTERN = Pattern.compile("[-+]?\\p{javaDigit}+");
    private static final Pattern LINE_PATTERN = Pattern.compile(".*(\r\n|[\n\r\u2028\u2029\u0085])|.+$");

    private final Readable source;
    private final Matcher matcher;
    private CharBuffer buf;
    private int position;
    private Pattern delimPattern;
    private boolean sourceClosed;
    private boolean needInput;
    private boolean skipped;
    private int savedScannerPosition = -1;

    private Scanner(final Readable source, final Pattern pattern) {
        this.source = source;
        this.delimPattern = pattern;
        this.buf = CharBuffer.allocate(BUFFER_SIZE);
        this.buf.limit(0);
        this.matcher = this.delimPattern.matcher(this.buf);
        this.matcher.useTransparentBounds(true);
        this.matcher.useAnchoringBounds(false);
    }

    public Scanner(final InputStream source, final String charsetName) {
        this(source, toCharset(charsetName));
    }

    private Scanner(final InputStream source, final Charset charset) {
        this(makeReadable(Objects.requireNonNull(source, "source"), charset), WHITESPACE_PATTERN);
    }

    public boolean hasNext() {
        this.saveState();
        while (!this.sourceClosed) {
            if (this.hasTokenInBuffer()) {
                return this.revertState(true);
            }
            this.readInput();
        }
        return this.revertState(this.hasTokenInBuffer());
    }

    public int nextInt() {
        try {
            return Integer.parseInt(this.next(INTEGER_PATTERN), 10);
        } catch (final NumberFormatException e) {
            this.position = this.matcher.start();
            throw new InputMismatchException(e.getMessage());
        }
    }

    public String next() {
        return this.next(null);
    }

    public String nextLine() {
        String result = this.findWithinHorizon(LINE_PATTERN, 0);
        if (result == null) {
            throw new NoSuchElementException("No line found");
        }
        final String lineSep = this.matcher.group(1);
        if (lineSep != null) {
            result = result.substring(0, result.length() - lineSep.length());
        }
        return result;
    }

    public Scanner useDelimiter(final String pattern) {
        this.delimPattern = Pattern.compile(pattern);
        return this;
    }

    private String next(final Pattern pattern) {
        while (true) {
            final String token = this.getCompleteTokenInBuffer(pattern);
            if (token != null) {
                this.skipped = false;
                return token;
            }
            if (this.needInput) {
                this.readInput();
            } else {
                this.throwFor();
            }
        }
    }

    private void saveState() {
        this.savedScannerPosition = this.position;
    }

    private boolean revertState(final boolean result) {
        this.position = this.savedScannerPosition;
        this.savedScannerPosition = -1;
        this.skipped = false;
        return result;
    }

    private void readInput() {
        if (this.buf.limit() == this.buf.capacity()) {
            this.makeSpace();
        }
        final int p = this.buf.position();
        this.buf.position(this.buf.limit());
        this.buf.limit(this.buf.capacity());

        int n;
        try {
            n = this.source.read(this.buf);
        } catch (final IOException _) {
            n = -1;
        }
        if (n == -1) {
            this.sourceClosed = true;
            this.needInput = false;
        }
        if (n > 0) {
            this.needInput = false;
        }
        this.buf.limit(this.buf.position());
        this.buf.position(p);
    }

    private boolean makeSpace() {
        final int offset = this.savedScannerPosition == -1 ? this.position : this.savedScannerPosition;
        this.buf.position(offset);
        if (offset > 0) {
            this.buf.compact();
            this.translateSavedIndexes(offset);
            this.position -= offset;
            this.buf.flip();
            return true;
        }
        final int newSize = this.buf.capacity() * 2;
        final CharBuffer newBuf = CharBuffer.allocate(newSize);
        newBuf.put(this.buf);
        newBuf.flip();
        this.translateSavedIndexes(offset);
        this.position -= offset;
        this.buf = newBuf;
        this.matcher.reset(this.buf);
        return true;
    }

    private void translateSavedIndexes(final int offset) {
        if (this.savedScannerPosition != -1) {
            this.savedScannerPosition -= offset;
        }
    }

    private void throwFor() {
        this.skipped = false;
        if (this.sourceClosed && this.position == this.buf.limit()) {
            throw new NoSuchElementException();
        }
        throw new InputMismatchException();
    }

    private boolean hasTokenInBuffer() {
        this.matcher.usePattern(this.delimPattern);
        this.matcher.region(this.position, this.buf.limit());
        if (this.matcher.lookingAt()) {
            if (this.matcher.hitEnd() && !this.sourceClosed) {
                this.needInput = true;
                return false;
            }
            this.position = this.matcher.end();
        }
        return this.position != this.buf.limit();
    }

    private String getCompleteTokenInBuffer(Pattern pattern) {
        this.matcher.usePattern(this.delimPattern);
        if (!this.skipped) {
            this.matcher.region(this.position, this.buf.limit());
            if (this.matcher.lookingAt()) {
                if (this.matcher.hitEnd() && !this.sourceClosed) {
                    this.needInput = true;
                    return null;
                }
                this.skipped = true;
                this.position = this.matcher.end();
            }
        }
        if (this.position == this.buf.limit()) {
            if (this.sourceClosed) {
                return null;
            }
            this.needInput = true;
            return null;
        }

        this.matcher.region(this.position, this.buf.limit());
        boolean foundNextDelim = this.matcher.find();
        if (foundNextDelim && this.matcher.end() == this.position) {
            foundNextDelim = this.matcher.find();
        }
        final int tokenEnd;
        if (foundNextDelim) {
            if (this.matcher.requireEnd() && !this.sourceClosed) {
                this.needInput = true;
                return null;
            }
            tokenEnd = this.matcher.start();
        } else if (this.sourceClosed) {
            tokenEnd = this.buf.limit();
        } else {
            this.needInput = true;
            return null;
        }
        if (pattern == null) {
            pattern = FIND_ANY_PATTERN;
        }
        this.matcher.usePattern(pattern);
        this.matcher.region(this.position, tokenEnd);
        if (this.matcher.matches()) {
            final String token = this.matcher.group();
            this.position = this.matcher.end();
            return token;
        }
        return null;
    }

    private boolean findPatternInBuffer(final Pattern pattern, final int horizon) {
        this.matcher.usePattern(pattern);
        final int bufferLimit = this.buf.limit();
        int horizonLimit = -1;
        int searchLimit = bufferLimit;
        if (horizon > 0) {
            horizonLimit = this.position + horizon;
            if (horizonLimit < bufferLimit) {
                searchLimit = horizonLimit;
            }
        }
        this.matcher.region(this.position, searchLimit);
        if (this.matcher.find()) {
            if (this.matcher.hitEnd() && !this.sourceClosed && (searchLimit != horizonLimit || this.matcher.requireEnd())) {
                this.needInput = true;
                return false;
            }
            this.position = this.matcher.end();
            return true;
        }
        if (this.sourceClosed) {
            return false;
        }
        if (horizon == 0 || searchLimit != horizonLimit) {
            this.needInput = true;
        }
        return false;
    }

    private String findWithinHorizon(final Pattern pattern, final int horizon) {
        while (true) {
            if (this.findPatternInBuffer(pattern, horizon)) {
                return this.matcher.group();
            }
            if (this.needInput) {
                this.readInput();
            } else {
                break;
            }
        }
        return null;
    }

    private static Charset toCharset(final String csn) {
        Objects.requireNonNull(csn, "charsetName");
        try {
            return Charset.forName(csn);
        } catch (final IllegalCharsetNameException | UnsupportedCharsetException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static Readable makeReadable(final InputStream source, final Charset charset) {
        Objects.requireNonNull(charset, "charset");
        return new InputStreamReader(source, charset);
    }

}
