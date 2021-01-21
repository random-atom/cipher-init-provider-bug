package org.randomatom;

import java.security.SecureRandom;
import java.security.Security;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.crypto.CryptoServicesRegistrar;
import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;

/**
 * Cipher.init(int, key) does not use highest priority provider for random bytes.
 */
public class CipherInitProviderBug {
    public static void run() throws Exception {
        // Bouncy Castle strict mode
        CryptoServicesRegistrar.setApprovedOnlyMode(true);

        SecretKey key = new SecretKeySpec(new byte[32], "AES");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key);

        BouncyCastleFipsProvider provider = new BouncyCastleFipsProvider();
        Security.insertProviderAt(provider, 1);
        cipher = Cipher.getInstance("AES/GCM/NoPadding");

        // Proof that init works fine with a freshly created SecureRandom instance
        System.out.println("init works fine with a freshly created SecureRandom instance");
        cipher.init(Cipher.ENCRYPT_MODE, key, new SecureRandom());

        // Proof that init fails with Cipher.init(int, key) because Cipher uses an existing static instance
        // created with the original Provider.
        System.out.println("init fails with the SecureRandom instance used by the Cipher class");
        cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
    }
}
