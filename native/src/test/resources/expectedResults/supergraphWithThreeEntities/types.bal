import ballerina/graphql;

public type Category record {|
    string id?;
    string title?;
|};

public type Product record {|
    Review[] reviews?;
    int price?;
    string description?;
    string id?;
    string title?;
    Category category?;
|};

public type Review record {|
    string author?;
    float rating?;
    string comment?;
    string id?;
|};

public type productResponse record {
    record {|Product product;|} data;
    graphql:ErrorDetail[] errors?;
};

public type productsResponse record {
    record {|Product[] products;|} data;
    graphql:ErrorDetail[] errors?;
};

public type reviewsResponse record {
    record {|Review[] reviews;|} data;
    graphql:ErrorDetail[] errors?;
};
