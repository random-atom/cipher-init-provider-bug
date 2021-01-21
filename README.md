# cipher-init-provider-bug

## Purpose

This project replicates a bug in Cipher.init(int, key) where the highest priority Provider is not used.

## Description:

The Javadoc for Cipher.init(int opmode, Key key) says the following:
"If this cipher (including its underlying feedback or padding scheme) requires any random bytes (e.g., for parameter
generation), it will get them using the SecureRandom implementation of the highest-priority installed provider as the
source of randomness."

However, the implementation of this method does this (I checked Oracle JDK v1.8 and Zulu JDK 11.0.9 based on OpenJDK):

```
public final void init(int opmode,Key key)throws InvalidKeyException {
    init(opmode,key,JceSecurity.RANDOM);
}
```

And this is JceSecurity.RANDOM:

```
static final SecureRandom RANDOM=new SecureRandom();
```

As a result, it doesn't matter what the highest priority provider is at the moment this method is called. It will always
pick a previously created instance that is associated with whatever provider was the highest priority at the time that
it was created.

## How to replicate the problem

The bug is clear from the source code, but it's hard to prove the bug using code because there's no simple way to query
the Cipher class for which SecureRandom instance it used. That said,
the [FIPS-certified Bouncy Castle Provider](https://mvnrepository.com/artifact/org.bouncycastle/bc-fips) is strict in
this regard (when configured appropriately) and throws `FipsUnapprovedOperationError` when it sees that the SecureRandom
instance given to it does not meet its strict criteria.

### Steps to replicate

Compile this project and run the test

```
mvn compile test
```

### What it does

1. Instantiate a Cipher object and initialize it using Cipher.init(int, key). It will use the default Provider.
2. Add a new provider at highest priority, one that supports SecureRandom. We add the "BCFIPS" Provider.
3. Instantiate a Cipher object and initialize it using Cipher.init(int, key).

### Expected result

The call in step 3 should succeed, creating a SecureRandom instance internally that uses the prioritized Provider.

The implementation should be the following (or, if the intent is to cache the instance, it should check if an instance
is cached for the current highest priority provider of SecureRandom):

```
public final void init(int opmode,Key key)throws InvalidKeyException {
    init(opmode,key,new SecureRandom());
}
```

### Actual result

FipsUnapprovedOperationError is thrown.

## Workaround

Call an init() method that accepts an explicit SecureRandom argument.

Unfortunately this workaround may not work for applications relying on libraries that make the Cipher.init() call.
