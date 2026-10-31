// This source must fail ECJ compilation under the production JDK module restrictions.
final class RestrictedJdkReference {
    sun.security.x509.X500Name forbidden;
}
