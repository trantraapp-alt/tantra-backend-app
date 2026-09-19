-- V10: User Wishlist (Favourites) — per-user list of saved listings.
-- Each row is one (user, listing) pair; unique constraint prevents duplicates.
-- favorite_count on the listings table is kept in sync by WishlistService.

CREATE TABLE user_wishlist (
    id          SERIAL       PRIMARY KEY,
    user_id     VARCHAR(20)  NOT NULL,
    listing_id  VARCHAR(20)  NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_wishlist UNIQUE (user_id, listing_id)
);

CREATE INDEX idx_user_wishlist_user    ON user_wishlist(user_id);
CREATE INDEX idx_user_wishlist_listing ON user_wishlist(listing_id);
