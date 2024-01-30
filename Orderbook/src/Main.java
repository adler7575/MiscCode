import javax.swing.tree.TreeNode;
import java.math.BigDecimal;
import java.util.Random;

public class Main {
    public static void main(String[] args) {
        int skiptest = 13;
        //TIP Press <shortcut actionId="ShowIntentionActions"/> with your caret at the highlighted text
        // to see how IntelliJ IDEA suggests fixing it.
        System.out.println("Hello and welcome!");
        Level2ViewCls cls = new Level2ViewCls();

        cls.onNewOrder(Level2View.Side.BID, BigDecimal.valueOf(10), 4, 1);
        cls.onNewOrder(Level2View.Side.BID, BigDecimal.valueOf(2), 4, 2);
        cls.onNewOrder(Level2View.Side.BID, BigDecimal.valueOf(5), 4, 3);
        cls.onNewOrder(Level2View.Side.BID, BigDecimal.valueOf(5), 4, 12);
        cls.onNewOrder(Level2View.Side.BID, BigDecimal.valueOf(5), 4, 13);
        cls.onNewOrder(Level2View.Side.BID, BigDecimal.valueOf(12), 4, 4);
        // Test1. Kolla så antalet prisnivåer stämmer
        if (skiptest < 1 && cls.getBookDepth(Level2View.Side.BID) != 4)  throw new RuntimeException ("Test 1 failed");
        // Test2. Kolla så att summan för prisnivån stämmer
        if (skiptest < 2 &&cls.getSizeForPriceLevel(Level2View.Side.BID, BigDecimal.valueOf(5)) != 12) throw new RuntimeException ("Test 2 failed");
        // Test3. Hämta ut nuvarande maxbuy
        if (skiptest < 3 && !cls.getTopOfBook(Level2View.Side.BID).equals(BigDecimal.valueOf(12))) throw new RuntimeException ("Test 3 failed");
        // Ta bort nuvarande maxbuy
        cls.onCancelOrder(4);
        // Test4. Kolla så antalet prisnivåer stämmer
        if (skiptest < 4 &&cls.getBookDepth(Level2View.Side.BID) != 3) throw new RuntimeException ("Test 4 failed");
        // Test5. Kontrollera att nuvarande maxbuy har justerats
        if (skiptest < 5 &&!cls.getTopOfBook(Level2View.Side.BID).equals(BigDecimal.valueOf(10))) throw new RuntimeException ("Test 5 failed");
        // Ta bort en order från en nivå
        cls.onCancelOrder(12);
        // Test6. Kolla så att summan för prisnivån ff stämmer
        if (skiptest < 5 &&cls.getSizeForPriceLevel(Level2View.Side.BID, BigDecimal.valueOf(5)) != 8) throw new RuntimeException ("Test 6 failed");

        cls.onNewOrder(Level2View.Side.ASK, BigDecimal.valueOf(30), 4, 5);
        cls.onNewOrder(Level2View.Side.ASK, BigDecimal.valueOf(22), 4, 6);
        cls.onNewOrder(Level2View.Side.ASK, BigDecimal.valueOf(25),  4, 7);
        cls.onNewOrder(Level2View.Side.ASK, BigDecimal.valueOf(27), 4, 8);
        // Test7. Kontrollera nuvarande minsell
        if (skiptest < 7 &&!cls.getTopOfBook(Level2View.Side.ASK).equals(BigDecimal.valueOf(22))) throw new RuntimeException ("Test 7 failed");
        // Ersätt en order map priset och kvantitet
        cls.onReplaceOrder(BigDecimal.valueOf(20),8,6 );
        // Test8. Kolla så ny minsell nivå erhållits.
        if (skiptest < 8 &&!cls.getTopOfBook(Level2View.Side.ASK).equals(BigDecimal.valueOf(20))) throw new RuntimeException ("Test 8 failed");
        // Test9. Kolla så att summan för prisnivån ff stämmer
        if (skiptest < 9 &&cls.getSizeForPriceLevel(Level2View.Side.ASK, BigDecimal.valueOf(20)) != 8) throw new RuntimeException ("Test 9 failed");

        cls.onNewOrder(Level2View.Side.ASK, BigDecimal.valueOf(18), 4, 100);
        cls.onNewOrder(Level2View.Side.ASK, BigDecimal.valueOf(18), 4, 101);
        // Test10. Kolla så ny minsell nivå erhållits.
        if (skiptest < 10 &&!cls.getTopOfBook(Level2View.Side.ASK).equals(BigDecimal.valueOf(18))) throw new RuntimeException ("Test 10 failed");
        cls.onCancelOrder(100);

        // Test11. Kolla så antalet prisnivåer stämmer
        if (skiptest < 11 &&cls.getBookDepth(Level2View.Side.ASK) != 5)  throw new RuntimeException ("Test 11 failed");


        cls.onTrade(2, 3);
        // Test12. Kolla så att rätt delmängd återstår
        if (skiptest < 12 && cls.getSizeForPriceLevel(Level2View.Side.BID, BigDecimal.valueOf(5)) != 6) throw new RuntimeException ("Test 12 failed");


        // Test 13. Lite prestanda tester
        cls = null;
        cls = new Level2ViewCls();
        Random rd = new Random();
        long MaxOrderNum = 20;
        long MaxOrderId = 5000000;
        for (long orderId = 0; orderId < MaxOrderNum;)
        {
            long l = (long) (rd.nextDouble() * MaxOrderId);
            long q = (long) (rd.nextDouble() * 10);

            int r = (int)(rd.nextDouble() * 5);
            for (int rp = 0; rp < r && orderId < MaxOrderNum; rp++) {
                cls.onNewOrder(Level2View.Side.ASK, BigDecimal.valueOf(l), q, orderId++);
            }
        }
        for (long orderId = 0; orderId < MaxOrderNum;orderId++)
        {
            cls.onCancelOrder(orderId);
        }

    }
}