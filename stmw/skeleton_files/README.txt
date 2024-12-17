1) Tables and Schemas:
Item Table:
ItemID (Primary Key)
Name
SellerID
Currently
Buy_price
First_bid
Location
Latitude
Longitude
Country
Started
Ends
Description
Seller Table:
UserID (Primary Key)
Rating
Bidder Table:
BidderID (Primary Key)
Rating
Location
Country
BidInfo Table:
ItemID (Foreign Key)
BidderID (Foreign Key)
Time
Amount
Category Table:
Category
ItemID (Foreign Key)

2) Functional Dependencies:
Item:
ItemID -> Name, SellerID, Currently, Buy_price, First_bid, Location, Latitude, Longitude, Country, Started, Ends, Description
Seller:
UserID -> Rating
Bidder:
BidderID -> Rating, Location, Country
BidInfo:
ItemID, BidderID, Time -> Amount
Category:
Category, ItemID

3) BCNF (Boyce-Codd Normal Form) and 4NF:
BCNF:
Item Table: ItemID, every column is fully functionally dependent on ItemID.
Seller Table: UserID -> Rating, so every column is fully dependent on UserID.
Bidder Table: BidderID -> Rating, Location, Country, making each column fully dependent on BidderID.
BidInfo Table: ItemID and BidderID together determine the record uniquely for each combination.
Category Table: Category and ItemID pair together to form the primary relationship.

4)
4NF Condition:
To satisfy 4NF, tables must not have multivalued dependencies (MVD). In the tables provided, no such dependencies exist. Each table avoids multivalued dependencies, so they comply with 4NF.

