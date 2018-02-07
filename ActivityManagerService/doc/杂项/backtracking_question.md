# backtracking_question #

##　概念　##

**backtracking**，即**回溯**，当判断条件不满足的时候退出，回到可以执行的结点。**DFS**是backtracking的一种策略。**递归**是一种行为，比如递归调用某一个函数，这个函数就可以是DFS。

## 回溯一般解决以下几个问题 ##

1. Find a path to success 有没有解
2. Find all paths to success 求所有解
  - 求所有解的个数
  - 求所有解的具体信息
3. Find the best path to success 求最优解

回溯可以抽象成一颗树，树的根节点是函数起始点，叶子节点是函数的终止节点。在没有到叶子节点的时候，树会不断的加深，在叶子节点的时候会根据条件判断是否到达叶子节点。而叶子节点也分作good leaf和bad leaf。

## 回溯问题的模型 ##

### 第一种 ###

Find a path to success,返回值是true/false。

```
boolean solve(Node n) {
    if n is a leaf node {
        if the leaf is a goal node, return true
        else return false
    } else {
        for each child c of n {
            if solve(c) succeeds, return true
        }
        return false
    }
}
```
从叶子节点返回值，回传到父节点。


### 第二种 ###

Find all paths to success，求个数，设全局counter，返回值是void；求所有解信息，设result，返回值void。

```
void solve(Node n) {
    if n is a leaf node {
        if the leaf is a goal node, count++, return;
        else return
    } else {
        for each child c of n {
            solve(c)
        }
    }
}
```
父节点不需要返回值，子节点对全局值进行count进程操作。这种类型的变式会比较多，后面会有例子。


### 第三种 ###

```
void solve(Node n) {
    if n is a leaf node {
        if the leaf is a goal node, update best result, return;
        else return
    } else {
        for each child c of n {
            solve(c)
        }
    }
}
```
在叶子节点的时候进行判断，是否是需要的目标节点，是的话与之前的进行匹配。



## 具体问题实例 ##

### 问题描述 ###

八皇后 N-Queens
问题
1.给个n，问有没有解；
2.给个n，有几种解；(Leetcode N-Queens II)
3.给个n，给出所有解；(Leetcode N-Queens I)

### 有没有解 ###

怎么做：一行一行的放queen，每行尝试n个可能，有一个可达，返回true；都不可达，返回false.

边界条件leaf:放完第n行 或者 该放第n+1行(出界，返回)

目标条件goal:n行放满且isValid，即目标一定在leaf上

### 求解的个数 ###

怎么做：一行一行的放queen，每行尝试n个可能。这回因为要找所有，返回值就没有了意义，用void即可。在搜索时，如果有一个可达，仍要继续尝试；每个子选项都试完了，返回.

边界条件leaf:放完第n行 或者 该放第n+1行(出界，返回)

目标条件goal:n行放满且isValid，即目标一定在leaf上

### 求所有解的具体信息 ###

怎么做：一行一行的放queen，每行尝试n个可能。返回值同样用void即可。在搜索时，如果有一个可达，仍要继续尝试；每个子选项都试完了，返回.

边界条件leaf:放完第n行 或者 该放第n+1行(出界，返回)

目标条件goal:n行放满且isValid，即目标一定在leaf上

### 问题描述 ###

[backtracking相关的问题](https://leetcode.com/problems/permutations/discuss/18239)

子集问题，排列问题，组合和，回文敏感等等问题。具体问题可以在leetcode上进行查找答案。