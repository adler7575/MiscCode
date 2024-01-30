
import java.math.BigDecimal;
import java.util.Map;
import java.util.HashMap;




public class Level2ViewCls implements Level2View{

    // Inner class for Order used to store information of an order
    private final class Order {
        // Properties
        Order _nextOrder;
        Order _prevOrder;
        long _orderId;
        BigDecimal _price;
        TreeNode _priceNode;
        long _quantity;

        Side _side;


        //Constructor for class Order
        Order(long orderId, BigDecimal price, TreeNode priceNode, long quantity, Side side)
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
    private final class TreeNode {
        // properties
        long _nodes_left, _nodes_right;
        BigDecimal _price;
        long _totalQuantity;

        //long _leftHeight;
        //long _rightHeight;
        TreeNode _left  // denotes child to the left
                , _right // denotes possible child to the right
                , _parent; // denotes parent in tree, used when recalculating Top of Book

        Order _headOrder, _tailOrder;

        //Constructors for class treeNode

        TreeNode(BigDecimal price) {
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
        priceMap = new HashMap<>();
        orderIdMap = new HashMap<>();
    }


    // Properties
    private TreeNode BuyTree;
    private TreeNode SellTree;

    private TreeNode LowestSell;
    private TreeNode HighestBuy;

    // Direct map for connecting a price to a node in either sell or buy tres.
    private final Map<BigDecimal, TreeNode> priceMap;
    // Direct map for connecting an orderId to a created or Order somewhere in the two trees.
    private final Map<Long,Order> orderIdMap;
    /// Methods

    // Inserts a new order into either sell och buy tree.
    // If the current price level is not available a new node is created and then the order is attached to it,
    // otherwise the order will be put last in the list of available orders.
    public void onNewOrder(Side side, BigDecimal price, long quantity, long orderId) {
        synchronized (orderIdMap) {
            TreeNode n = priceMap.get(price);
            // If  a node already exists, use it and add another order to it
            if (n != null) {
                System.out.println("adding a order to a node");
                Order o = new Order(orderId, price, n, quantity, n._headOrder._side);
                o._prevOrder = n._tailOrder;
                n._tailOrder._nextOrder = o;
                n._tailOrder = o;
                o._nextOrder = null;
                orderIdMap.put(orderId, o);
                n._totalQuantity += quantity;

            } else {
                System.out.println("adding a new order node");
                int cond = -1;
                n = new TreeNode(price);
                priceMap.put(price, n);

                // Now first insert the new treeNode into the right tree
                TreeNode searchNode;
                // Check if tree has been created or not before
                if (side == Side.BID) {
                    if (BuyTree == null) {

                        searchNode = HighestBuy = BuyTree = n;
                        cond = 0;
                    } else {
                        searchNode = BuyTree;
                    }
                } else {
                    if (SellTree == null) {
                        searchNode = LowestSell = SellTree = n;
                        cond = 0;
                    } else {
                        searchNode = SellTree;
                    }
                }
                // Tricky condition here, but here to prevent us going further with if statement
                // when we only have one node in the tree; the newly created n.
                while (cond != 0) {
                    if (searchNode._left != null && searchNode._price.compareTo(price) > 0) {
                        searchNode = searchNode._left;
                    } else if (searchNode._price.compareTo(price) > 0) {
                        cond = 1;
                        break;
                    } else if (searchNode._right != null && searchNode._price.compareTo(price) < 0) {
                        searchNode = searchNode._right;
                    } else if (searchNode._price.compareTo(price) < 0) {
                        cond = 2;
                        break;
                    }

                }
                if (cond != 0) {
                    n._parent = searchNode;
                    if (cond == 1) {
                        searchNode._left = n;
                    } else {
                        searchNode._right = n;
                    }
                }
                // Then create a new order
                Order o = new Order(orderId, price, n, quantity, side);
                n._headOrder = o;
                n._tailOrder = o;

                orderIdMap.put(orderId, o);
                n._totalQuantity = quantity;
                if (side == Side.BID) {
                    if (n != BuyTree)
                        if (price.compareTo(BuyTree._price) < 0)
                            BuyTree._nodes_left++;
                        else
                            BuyTree._nodes_right++;
                    // Fix a potential new highest buy
                    if (price.compareTo(HighestBuy._price) > 0)
                        HighestBuy = n;
                } else {
                    if (n != SellTree)
                        if (price.compareTo(SellTree._price) < 0)
                            SellTree._nodes_left++;
                        else
                            SellTree._nodes_right++;
                    // Fix a potential new lowest sell
                    if (price.compareTo(LowestSell._price) < 0)
                        LowestSell = n;

                }
            }
        }
    }

    // Remove an order from either Sell or Buy tree
    // Potentially removes a node from the corresponding tree.
    // Otherwise updates total number of records per node, as well taking care of fixing a potential new
    // lowestSale och HighestBuy
    public void onCancelOrder(long orderId) {

        synchronized (orderIdMap) {
            Order o = orderIdMap.get(orderId);

            o._priceNode._totalQuantity -= o._quantity;
            // Should we remove the whole node, as only one remains?
            if (o._priceNode._tailOrder == o._priceNode._headOrder) {
                // Adjust the number of elements to right and left for use with depth of Book
                if (getBookDepth(o._side)>1) {
                    if (o._side == Side.BID) {
                        if (BuyTree._nodes_left > 0 &&o._priceNode._price.compareTo(BuyTree._price) < 0)
                            BuyTree._nodes_left--;
                        else if (BuyTree._nodes_right > 0)
                            BuyTree._nodes_right--;
                    } else {
                        if (SellTree._nodes_left > 0 && o._priceNode._price.compareTo(SellTree._price) < 0)
                            SellTree._nodes_left--;
                        else if (SellTree._nodes_right > 0)
                            SellTree._nodes_right--;
                    }
                }
                // Unmap from pricemap structure
                priceMap.remove(o._priceNode._price);
                // Fix changes in lowestsell, highestbuy
                if (o._side == Side.BID) {
                    if (o._priceNode == BuyTree && BuyTree._nodes_right == 0 && BuyTree._nodes_left ==0)
                        BuyTree = HighestBuy = null;
                    else if (o._priceNode == HighestBuy)
                        HighestBuy = HighestBuy._parent;
                } else {
                    if (o._priceNode == SellTree  && SellTree._nodes_right == 0 && SellTree._nodes_left ==0)
                        SellTree = LowestSell = null;
                    else if (o._priceNode == LowestSell)
                        LowestSell = LowestSell._parent;
                }
                // Now to the semi tricky part, remove the node itself.
                o._priceNode = deleteNode(o._priceNode);
                if (o._side == Side.BID && BuyTree == null)
                    BuyTree = o._priceNode;
                else if (o._side == Side.ASK && SellTree == null)
                    SellTree = o._priceNode;
            } // The node should remain but order needs to be removed
            else { // Three cases, either with  we have a headorder, tailorder or an order in the middle
                if (o._priceNode._headOrder == o) {
                    System.out.println("Deleting a headnode");
                    Order temp = o._priceNode._headOrder;
                    o._priceNode._headOrder = o._priceNode._headOrder._nextOrder;
                    o._priceNode._headOrder._prevOrder = null;
                    temp._nextOrder = null;
                } else if (o._priceNode._tailOrder == o) {
                    System.out.println("Deleting a tailnode");
                    Order temp = o._priceNode._tailOrder;
                    o._priceNode._tailOrder = o._priceNode._tailOrder._prevOrder;
                    o._priceNode._tailOrder._nextOrder = null;
                    temp._prevOrder = null;

                } else {
                    System.out.println("Deleting a midnode");
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
    }

    // Replaces an order by first deleting it and then inserting it again
    public void onReplaceOrder(BigDecimal price, long quantity, long orderId)
    {
        synchronized (orderIdMap) {

            Order o = orderIdMap.get(orderId);
            Side s = o._side;
            onCancelOrder(orderId);
            onNewOrder(s, price, quantity, orderId);
        }
    }
    // When an aggressor order crosses the spread, it will be matched with an existing resting order,
    // causing a trade.
    // The aggressor order will NOT cause an invocation of onNewOrder.
    public void onTrade(long quantity, long restingOrderId)
    {
        synchronized (orderIdMap) {
            Order o = orderIdMap.get(restingOrderId);
            o._quantity -= quantity;
            o._priceNode._totalQuantity -= quantity;
            if (o._quantity == 0)
                onCancelOrder(restingOrderId);
        }
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
        return SellTree==null?0:1 + SellTree._nodes_left + SellTree._nodes_right;
    }

    // get highest bid or lowest ask, resp
    public BigDecimal getTopOfBook(Side side)
    {
        if (side == Side.BID)
            return HighestBuy == null? null:HighestBuy._price;
        return LowestSell == null? null: LowestSell._price;
    }

    private TreeNode deleteNode(TreeNode root) {
        // Base case
        if (root == null)
            return root;

        // If one of the children is empty
        if (root._left == null) {
            return root._right;
        } else if (root._right == null) {
            return root._left;
        }

        // If both children exist
        else {

            TreeNode succParent = root;

            // Find successor
            TreeNode succ = root._right;
            while (succ._left != null) {
                succParent = succ;
                succ = succ._left;
            }

            // Delete successor.  Since successor
            // is always left child of its parent
            // we can safely make successor's right
            // right child as left of its parent.
            // If there is no succ, then assign
            // succ.right to succParent.right
            if (succParent != root)
                succParent._left = succ._right;
            else
                succParent._right = succ._right;

            // Copy Successor Data to root
            root._price = succ._price;
            root._nodes_right = succ._nodes_right;
            root._nodes_left = succ._nodes_left;
            root._tailOrder = succ._tailOrder;
            root._headOrder = succ._tailOrder;
            root._totalQuantity = succ._totalQuantity;
            // Delete Successor and return root
            return root;
        }
    }


}

