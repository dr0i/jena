/*
 * (c) Copyright 2011 Epimorphics Ltd.
 * All rights reserved.
 * [See end of file]
 */

package dev;

import java.io.InputStream ;
import java.io.Reader ;

import org.openjena.atlas.io.IO ;
import org.openjena.atlas.io.PeekReader ;
import org.openjena.riot.RiotParseException ;
import org.openjena.riot.system.RiotChars ;
import org.openjena.riot.tokens.Token ;
import org.openjena.riot.tokens.TokenType ;

import com.hp.hpl.jena.sparql.util.Timer ;

/** NTriples parser written for speed. */ 
public final class LangNTriples3
{
    public static void main(String... argv) 
    {
        // Same as LangNTriples2 but works in char space.
        
        if ( argv.length == 0 )
            argv = new String[] {"-"} ;
        
        for ( String filename : argv )
            processOneFile(filename) ;
    }
    
    private static void processOneFile(String filename)
    {
        Timer timer = new Timer() ;
        InputStream in = IO.openFile(filename) ;

        // The byte-space one is 267K TPS
        // PeekReader uses CharStream - is that an overhead too much? 
        
        // 253K TPS - control the buffer size.
        Reader r = IO.asUTF8(in) ;
        PeekReader peek = PeekReader.make(r, 128*1024) ;
        
        // Slower: e.g. 190K TPS
//        InputStreamBuffered in2 = new InputStreamBuffered(in, 128*1024) ;
//        Reader r = new InStreamUTF8(in2) ;
//        PeekReader peek = PeekReader.make(r) ;
        
//        // Buffer in byte space.
//        InputStreamBuffered in2 = new InputStreamBuffered(in, 8*1024) ;
//        Reader r = IO.asUTF8(in) ;
//        // And in char space 
//        PeekReader peek = PeekReader.make(r) ;
        
        LangNTriples3 parser = new LangNTriples3(peek) ;
        timer.startTimer() ;
        
        try {
            long numberTriples = parser.parse() ;
            long timeMillis = timer.endTimer() ;

            double timeSec = timeMillis/1000.0 ;
            System.out.printf("%s : %,5.2f sec  %,d %s  %,.2f %s\n",
                              filename,
                              timeMillis/1000.0, numberTriples,
                              "triples",
                              timeSec == 0 ? 0.0 : numberTriples/timeSec,
                              "TPS") ;
        } catch (RiotParseException ex) { System.out.flush() ; throw ex ; }
    }
    
    final PeekReader input ;
    long line = 0 ;
    long col = 0 ;
    long count = 0 ;
    
    public LangNTriples3(PeekReader input) { this.input = input ; }

    private long parse()
    {
        for ( ;; )
        {
            skipWS() ;
            int ch = input.peekChar() ;
            if ( ch == -1 ) return count ; 
            if ( ch == '#' )
            {
                skipToLineEnd() ;
                continue ;
            }
            // Checking.
            Token s = token() ;
            skipWS() ;
            Token p = token() ;
            skipWS() ;
            Token o = token() ;
            skipWS() ;
            ch = input.peekChar() ;
            if ( ch != '.' )
                throw new RiotParseException("Triple not terminated by DOT ("+(char)ch+") ["+count+"]", line, col) ;
            input.readChar() ;
            skipWS() ;
            ch = input.readChar() ;
            if ( ch != '\n' )
                throw new RiotParseException("Triple not terminated by DOT-NL", line, col) ;
            
            //System.out.printf("%s %s %s .\n", s, p, o) ;
            count++ ;
        }
    }
    

    final StringBuilder sbuff = new StringBuilder(200) ;
    
    // Basic tokenizer, in byte space.
    // NT only.
    // No \ u processing.
    // next: work on chars to see the difference. 
    
    private Token token()
    {
        sbuff.setLength(0) ;
        int ch = input.peekChar() ;
        if ( ch == '<' )
        {
            input.readChar() ;
            for(;;)
            {
                ch = input.readChar() ;
                if ( ch == '>' )
                    break ;
                sbuff.append((char)ch) ;
                
            }
            Token t = new Token(line, col) ;
            t.setType(TokenType.IRI) ;
            t.setImage(sbuff.toString()) ;
            return t ;
        }
        else if ( ch == '_' )
        {
            input.readChar() ;
            for(;;)
            {
                // TODO Better
                ch = input.peekChar() ;
                if ( ! RiotChars.isA2ZN(ch) && ch != '-' && ch != ':' )
                    break ;
                sbuff.append((char)ch) ;
                input.readChar() ;
            }
            Token t = new Token(line, col) ;
            t.setType(TokenType.BNODE) ;
            t.setImage(sbuff.toString()) ;
            return t ;
        }
        else if ( ch == '"')
        {
            input.readChar() ;
            for(;;)
            {
                ch = input.peekChar() ;
                input.readChar() ;
                if ( ch == '"' )
                    break ;
                sbuff.append((char)ch) ;
                // Escape
                if ( ch == '\\' )
                {
                    ch = input.readChar() ;
                    sbuff.append((char)ch) ;
                }
            }
            // We skipped the "
//            input.readChar() ;
            ch = input.peekChar() ;
            
            if ( ch == '^' )
            {
                input.readChar() ;
                ch = input.peekChar() ;
                if ( ch != '^' )
                    throw new RiotParseException("Syntax error in datatype literal after ^", line, col) ;
                input.readChar() ;
                Token t2 = token() ;
                if ( ! t2.hasType(TokenType.IRI) )
                    throw new RiotParseException("Synatx error in datatype: IRI expected", line, col) ;
                Token t = new Token(line, col) ;
                t.setType(TokenType.LITERAL_DT) ;
                t.setImage(sbuff.toString()) ;
                t.setSubToken(t2) ;
                return t ;
            }
            else if ( ch == '@' )
            {
                input.readChar() ;
                String s = getLang() ;
                Token t = new Token(line, col) ;
                t.setType(TokenType.LITERAL_LANG) ;
                t.setImage(sbuff.toString()) ;
                t.setImage2(s) ;
                return t ;
            }
            else
            {
                Token t = new Token(line, col) ;
                t.setType(TokenType.STRING2) ;
                t.setImage(sbuff.toString()) ;
                return t ;
            }
        }
        else
            throw new RiotParseException("Unrecognized: "+ch, line, col) ;
    }
        
    private String getLang()
    {
        sbuff.setLength(0) ;
        for ( ;; )
        {
            int x = input.peekChar() ;
            if ( ! RiotChars.isA2Z(x) && x != '-' )
                break ;
            input.readChar() ;
            sbuff.append((char)x) ;
        }
        return sbuff.toString() ;
    }

    private void skipToLineEnd()
    {
        for ( ;; )
        {
            int x = input.peekChar() ;
            if ( x != '\n' && x != -1 )
                continue ;
            input.readChar() ;
        }

    }

    private void skipWS()
    {
        for ( ;; )
        {
            int x = input.peekChar() ;
            if ( x != ' ' && x != '\t' )
                return ;
            input.readChar() ;
        }
    }            

    //@Override
    public void remove()
    { throw new UnsupportedOperationException() ; }
}

/*
 * (c) Copyright 2011 Epimorphics Ltd.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */