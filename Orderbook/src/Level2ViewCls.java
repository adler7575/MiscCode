
import java.math.BigDecimal;
import java.security.InvalidParameterException;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

public class Level2ViewCls implements Level2View{

    // Inner class for Order used to store information of an order
    class Order {
        // Properties
        Order _nextOrder;
        Order _prevOrder;
        long _orderId;
        BigDecimal _price;
        treeNode _priceNode;
        long _quantity;

        Side _side;


        //Constructor for class Order
        Order(long orderId, BigDecimal price, treeNode priceNode, long quantity, Side side)
        {
            _orderId = orderId;
            _price = price;
            _priceNode = priceNode;
            _quantity = quantity;
            _side = side;
            _nextOrder = null;
            _prevOrder = null;
        }

    }
    // Inner class treeNode, just store information about a node in binary tree, where each node contains
    class treeNode {
        // properties
        long _nodes_left, _nodes_right;
        BigDecimal _price;
        long _totalQuantity;
        long _totalLeftQuantity;
        long _totalRightQuantity;
        treeNode _left  // denotes child to the left
                , _right // denotes possible child to the right
                , _parent; // denotes parent in tree, used when recalculating Top of Book

        Order _headOrder, _tailOrder;

        //Constructor for class treeNode
        treeNode(BigDecimal price) {
            _price = price;
            _totalQuantity = -1;
            _left = _right = null;
            _headOrder = null;
            _tailOrder = null;
            _nodes_left = 0;
            _nodes_right = 0;
        }

    }
    // Def. constructor
    public Level2ViewCls()
    {
        BuyTree = null;
        SellTree = null;
        LowestSell = null;
        HighestBuy = null;
        priceMap = new HashMap<BigDecimal, treeNode>();
        orderIdMap = new HashMap<Long, Order>();
    }


    // Properties
    treeNode BuyTree;
    treeNode SellTree;

    treeNode LowestSell;
    treeNode HighestBuy;


    // Direct map for connecting a price to an node in either sell or buy tres.
    Map<BigDecimal,treeNode> priceMap;
    // Direct map for connecting an orderId to a created or Order somewhere in the two trees.
    Map<Long,Order> orderIdMap;
    /// Methods

    // Inserts a new order into either sell och buy tree.
    // If the current price level is not available a new node is created and the the order is attached to it,
    // otherwise the order will be put last in the list of available orders.
    public void onNewOrder(Side side, BigDecimal price, long quantity, long orderId)
    {
        treeNode n = priceMap.get(price);
        // If  a node already exists, use it and add another order to it
        if (n != null)
        {
            Order o = new Order(orderId, price, n, quantity, n._headOrder._side);
            o._prevOrder = n._tailOrder;
            n._tailOrder._nextOrder = o;
            n._tailOrder = o;
            o._nextOrder = null;
            orderIdMap.put(orderId, o);
            n._totalQuantity += quantity;

            n._totalLeftQuantity += quantity;
        }
        else
        {
            int cond = -1;
            n = new treeNode(price);
            priceMap.put(price, n);

            // Now first insert the new treeNode into the right tree
            treeNode searchNode = null;
            // Check if tree has been created or not before
            if (side==Side.BID)
            {
                if (BuyTree == null)
                    searchNode = BuyTree = HighestBuy = n;
                else {
                    searchNode = BuyTree;
                    cond = 0;
                }
            }
            else
            {
                if (SellTree == null)
                    searchNode = SellTree = LowestSell = n;
                else {
                    searchNode = SellTree;
                    cond = 0;
                }
            }
            // Tricky condition here, but here to prevent us going further with if statement
            // when we only have one node in the tree; the newly created n.
            while (cond != 0)
            {
                if (searchNode._price.compareTo(price) > 0  && searchNode._left != null)
                {
                    searchNode = searchNode._left;
                    continue;
                }
                else if (searchNode._price.compareTo(price) > 0)
                {
                    cond = 1;
                    break;
                }
                else if (searchNode._price.compareTo(price) < 0 && searchNode._right != null)
                {
                    searchNode = searchNode._right;
                    continue;
                }
                else if (searchNode._price.compareTo(price) < 0)
                {
                    cond = 2;
                    break;
                }

            }
            if (cond != 0)
            {
                if (cond == 1) {
                    n._parent = searchNode;
                    searchNode._left = n;
                }
                else {
                    n._parent = searchNode;
                    searchNode._right = n;
                }
            }
            // Then create a new order
            Order o = new Order(orderId, price, n, quantity,side);
            n._headOrder = o;
            n._tailOrder = o;

            orderIdMap.put(orderId, o);
            n._totalQuantity = quantity;
            if (side == Side.BID)
            {
                BuyTree._totalQuantity += quantity;
                if (BuyTree._price.compareTo(price) < 0)
                    BuyTree._nodes_left++;
                else
                    BuyTree._nodes_right++;
                // Fix a potential new highest buy
                if (price.compareTo(HighestBuy._price) > 0)
                    HighestBuy = n;
            }
            else
            {
                SellTree._totalQuantity += quantity;
                if (SellTree._price.compareTo(price) < 0)
                    SellTree._nodes_left++;
                else
                    SellTree._nodes_right++;
                // Fix a potential new lowest sell
                if (price.compareTo(LowestSell._price) < 0)
                    LowestSell = n;

            }
        }
    }

    // Remove an order from either Sell or Buy tree
    // Takes order of updating total number of records per node as well as taking care of fixing a potential new
    // lowestSale och HighestBuy
    public void onCancelOrder(long orderId) {
        Order o = orderIdMap.get(orderId);

        o._priceNode._totalQuantity -= o._quantity;
        // Should we remove the whole node, as only one remains?
        if (o._priceNode._tailOrder == o._priceNode._headOrder) {
            if (o._side == Side.BID) {
                if (o._priceNode._price.compareTo(BuyTree._price) < 0)
                    BuyTree._nodes_left--;
                else
                    BuyTree._nodes_right--;
            }
            else {
                if (o._priceNode._price.compareTo(BuyTree._price) < 0)
                    SellTree._nodes_left--;
                else
                    SellTree._nodes_right--;
            }
            treeNode pnode = o._priceNode;
            // Unmap from map structure first
            priceMap.remove(pnode._price);
            // Fix changes in lowestsell, highestbuy
            if (o._side == Side.BID) {
                if (o._priceNode == BuyTree)
                    BuyTree = HighestBuy = null;
                else if (o._priceNode == HighestBuy)
                    HighestBuy = HighestBuy._parent;
            }
            else {
                if (o._priceNode == SellTree)
                    SellTree = LowestSell = null;
                else if (o._priceNode == LowestSell)
                    LowestSell = LowestSell._parent;
            }

            if (pnode._right != null)
                o._priceNode = pnode._right;
            else if (pnode._left != null)
                o._priceNode = pnode._left;
            else
                o._priceNode = null;
        }
        else { // Three cases, either with  we have an headorder, tailorder or an order in the middle
            if (o._priceNode._headOrder == o) {
                Order temp = o._priceNode._headOrder;
                o._priceNode._headOrder = o._priceNode._headOrder._nextOrder;
                o._priceNode._headOrder._prevOrder = null;
                temp._nextOrder = null;
            }
            else if (o._priceNode._tailOrder == o) {
                Order temp = o._priceNode._tailOrder;
                o._priceNode._tailOrder = o._priceNode._tailOrder._prevOrder;
                o._priceNode._tailOrder._nextOrder = null;
                temp._prevOrder = null;

            }
            else {
                o._prevOrder._nextOrder = o._nextOrder;
                o._nextOrder._prevOrder = o._prevOrder;
                o._prevOrder = null;
                o._nextOrder = null;
            }
        }
        // finally unref the order from the map
        orderIdMap.remove(orderId);
        o = null;

    }

    // Replaces an order by first deleting it and then inserting it again
    public void onReplaceOrder(BigDecimal price, long quantity, long orderId)
    {
        Order o = orderIdMap.get(orderId);
        Side s = o._side;
        onCancelOrder(orderId);
        onNewOrder(s, price, quantity, orderId);
    }
    // When an aggressor order crosses the spread, it will be matched with an existing resting order,
    // causing a trade.
    // The aggressor order will NOT cause an invocation of onNewOrder.
    public void onTrade(long quantity, long restingOrderId) throws InvalidParameterException
    {
        Order o = orderIdMap.get(restingOrderId);
        if (o == null) throw new InvalidParameterException("Invalid orderId supplied");
        if (quantity > o._quantity) throw new InvalidParameterException("Invalid quantity supplied");
        o._quantity -= quantity;
        o._priceNode._totalQuantity -= quantity;
        if (o._quantity == 0)
            onCancelOrder(restingOrderId);

    }

    // total quantity of existing orders on this price level
    public long getSizeForPriceLevel(Side side, BigDecimal price)
    {
        if (priceMap.get(price) == null) return 0;
        return priceMap.get(price)._totalQuantity;
    }

    // get the number of price levels on the specified side
    public long getBookDepth(Side side)
    {
        if (side == Side.BID)
            return BuyTree==null?0:1+BuyTree._nodes_left + BuyTree._nodes_right;
        return SellTree==null?0:1 + SellTree._nodes_right + SellTree._nodes_right;
    }

    // get highest bid or lowest ask, resp
    public BigDecimal getTopOfBook(Side side)
    {
        if (side == Side.BID)
            return HighestBuy == null? null:HighestBuy._price;
        return LowestSell == null? null: LowestSell._price;
    }

}

