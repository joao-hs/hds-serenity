package pt.ulisboa.tecnico.hdsledger.utilities;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Stack;

public class MerkleTree {
    public class Node {
        protected String hash;
        protected Node left;
        protected Node right;
        protected Node parent;

        public String getHash() {
            return hash;
        }

        public Node getLeft() {
            return left;
        }

        public Node getRight() {
            return right;
        }

        public Node getParent() {
            return parent;
        }
    }

    private Node root = null;

    public MerkleTree(Collection<String> fullLeaves) {
        if (fullLeaves.size() == 0) {
            return;
        }
        root = buildTree(fullLeaves);
    }

    private Node buildTree(Collection<String> fullLeaves) {
        ArrayList<Node> hashLeaves = fullLeaves.stream().map(l -> {
            Node leaf = new Node();
            try {
                leaf.hash = RSAEncryption.digest(l);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            return leaf;
        }).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        ArrayList<Node> parents = new ArrayList<>();
        while (hashLeaves.size() > 1) {
            parents.clear();
            for (int i = 0; i < hashLeaves.size(); i += 2) {
                Node parent = new Node();
                parent.left = hashLeaves.get(i);
                hashLeaves.get(i).parent = parent;
                if (i + 1 < hashLeaves.size()) {
                    parent.right = hashLeaves.get(i + 1);
                    hashLeaves.get(i + 1).parent = parent;
                } else {
                    parent.right = hashLeaves.get(i);
                    hashLeaves.get(i).parent = parent;
                }
                parent.hash = parent.left.hash + parent.right.hash;
                parents.add(parent);
            }
            hashLeaves = parents;
        }

        return hashLeaves.get(0);
    }

    public String getRoot() {
        return root.hash;
    }

    public ArrayList<String> getProof(String fullLeaf) {
        try {
            String hash = RSAEncryption.digest(fullLeaf);
            Node node = this.dfs(hash);
            if (node == null) {
                return null;
            }
            ArrayList<String> proof = new ArrayList<>();
            while (node.parent != null) {
                if (node.parent.left == node) {
                    proof.add(node.parent.right.hash);
                } else {
                    proof.add(node.parent.left.hash);
                }
                node = node.parent;
            }
            return proof;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean verifyProof(String fullLeaf, String merkleRootHash, ArrayList<String> proof) {
        try {
            String hash = RSAEncryption.digest(fullLeaf);
            for (String siblingHash : proof) {
                hash = hash + siblingHash;
                hash = RSAEncryption.digest(hash);
            }
            return hash.equals(merkleRootHash);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return false;
    }

    private Node dfs(String hash) {
        Stack<Node> stack = new Stack<>();
        stack.push(root);

        while (!stack.isEmpty()) {
            Node node = stack.pop();
            if (node.hash.equals(hash)) {
                return node;
            }
            if (node.left != null) {
                stack.push(node.left);
            }
            if (node.right != null) {
                stack.push(node.right);
            }
        }
        return null;
    }

}
