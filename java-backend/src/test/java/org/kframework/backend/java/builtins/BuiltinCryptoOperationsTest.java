// Copyright (c) 2016 K Team. All Rights Reserved.
package org.kframework.backend.java.builtins;

import org.apache.commons.codec.binary.Hex;
import org.junit.Test;
import org.kframework.backend.java.kil.TermContext;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;


public class BuiltinCryptoOperationsTest {

    /**
     * https://ethereum.stackexchange.com/questions/550/which-cryptographic-hash-function-does-ethereum-use
     *
     * Keccak Hash of the Word "testing"
     */
    private static final String testingKeccakHash = "5f16f4c7f149ac4f9510d9cf8cf384038ad348b3bcdc01915f95de12df9d1b02";
    @Mock
    TermContext context;

    @Test
    public void emptyKeccakDigestTest() {
        String hexString = Hex.encodeHexString(("testing").getBytes());
        StringToken in = StringToken.of(hexString);
        StringToken digest = BuiltinCryptoOperations.keccak256(in, context);
        assertEquals("Empty Digests Don't match", testingKeccakHash, digest.stringValue());
    }

}
