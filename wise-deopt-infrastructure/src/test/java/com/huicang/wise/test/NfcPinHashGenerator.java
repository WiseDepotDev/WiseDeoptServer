import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.util.UUID;

public class NfcPinHashGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

        String pin = "123456";
        String pinHash = encoder.encode(pin);

        String pinSalt = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        System.out.println("=================================");
        System.out.println("NFC Badge PIN Hash Generator");
        System.out.println("=================================\n");

        System.out.println("PIN: " + pin);
        System.out.println("PIN Hash: " + pinHash);
        System.out.println("PIN Salt: " + pinSalt);
        System.out.println();

        System.out.println("Verification:");
        System.out.println("============");
        System.out.println("Verify 123456: " + encoder.matches(pin, pinHash));
    }
}
