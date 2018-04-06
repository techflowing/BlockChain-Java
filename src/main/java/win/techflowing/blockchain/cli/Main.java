package win.techflowing.blockchain.cli;

/**
 * 类描述
 *
 * @author techflowing
 * @version v1.0
 * @since 2018/4/6
 */
public class Main {
    public static void main(String[] args) {
        CLI cli = new CLI(args);
        cli.parse();
    }
}
