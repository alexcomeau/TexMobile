package ca.alexcomeau.texmobile;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

import ca.alexcomeau.texmobile.blocks.*;

public class GameManager implements Parcelable {
    private Block currentBlock, nextBlock;
    private Board gameBoard;
    private int level;
    private int score;
    private int lockCurrent;
    private int droppedLines;
    private int combo;
    private boolean gameOver;
    private int spawnWait;
    private int fallWait;
    private int movementWait;
    private long startTimeMS;
    private boolean grandmasterValid, check1, check2, check3;
    private boolean redraw;

    // The number of frames between each drop
    private int gravity;
    // The number of drops per frame, once gravity reaches 1
    private int superGravity;
    // When the game ends
    private int maxLevel;

    // The number of frames after the block touches the stack before it locks.
    private final int LOCK_DELAY = 15;
    // Where pieces spawn
    private final Coordinate START = new Coordinate(3,17);
    // How many frames to wait after a piece is locked to spawn a new one.
    private final int SPAWN_DELAY = 15;
    // How many frames to wait between inputs
    private final int MOVEMENT_DELAY = 2;

    public GameManager(){ }

    // Start the game
    public void start(int levelStart, int levelEnd)
    {
        gameBoard = new Board();
        score = 0;
        level = 0;
        maxLevel = levelEnd;
        // Spawn the first piece immediately
        spawnWait = SPAWN_DELAY;
        movementWait = 0;
        addLevel(levelStart);
        combo = 1;
        gameOver = false;
        nextBlock = generateNewBlock();
        startTimeMS = System.currentTimeMillis();

        // If they're doing a full game they can attain grandmaster rank
        grandmasterValid = (maxLevel == 999 && levelStart == 0);
        check1 = grandmasterValid;
        check2 = grandmasterValid;
        check3 = grandmasterValid;
    }

    // Move ahead a frame
    public void advanceFrame(String input)
    {
        // No point evaluating movement and such if there's no piece to manipulate.
        boolean downtime = false;

        // If a block was placed last frame, swap it for the next one and generate a new next.
        if(currentBlock == null)
        {
            if(spawnWait++ >= SPAWN_DELAY)
            {
                currentBlock = nextBlock;
                nextBlock = generateNewBlock();

                // A new block appearing increases the level by one, unless the level ends in 99 or is the second last
                if ((level + 1) % 100 == 0 || level == maxLevel - 1)
                    level++;
            }
            else
                downtime = true;
        }

        if(!downtime) {
            droppedLines = 0;

            // Manipulate the block according to the user's input.
            if (movementWait++ >= MOVEMENT_DELAY)
            {
                movementWait = 0;

                switch (input)
                {
                    case "drop":
                        drop();
                        break;
                    case "left":
                        moveLeft();
                        break;
                    case "right":
                        moveRight();
                        break;
                    case "rotateLeft":
                        rotateLeft();
                        break;
                    case "rotateRight":
                        rotateRight();
                        break;
                    default:
                        break;
                }
            }

            // Check if the block is currently in a state of falling
            if (gameBoard.checkDown(currentBlock))
            {
                lockCurrent = LOCK_DELAY;
                // Move the block down if enough time has passed
                if (gravity > 0)
                    if (fallWait++ >= gravity)
                    {
                        fallWait = 0;
                        currentBlock.moveDown();
                    }
                    else
                        for (int i = 0; i < superGravity; i++)
                            if (gameBoard.checkDown(currentBlock))
                                currentBlock.moveDown();
            }
            else
            {
                // Check if the block needs to be locked
                if (lockCurrent-- <= 0)
                {
                    gameBoard.lockBlock(currentBlock);
                    lockCurrent = LOCK_DELAY;
                    // Check if locking that piece caused any lines to be cleared
                    checkClears();
                    currentBlock = null;
                    spawnWait = 0;
                    return;
                }
            }
        }
    }

    // ===== Input handling methods ==========================================
    private void drop()
    {
        // Count the number of lines the piece falls, but always at least one for scoring purposes
        droppedLines = 1;

        while(gameBoard.checkDown(currentBlock))
        {
            currentBlock.moveDown();
            droppedLines++;
        }

        // Set the current lock delay to one, making the piece lock immediately
        lockCurrent = 1;
    }

    private void moveLeft()
    {
        if(gameBoard.checkLeft(currentBlock))
        {
            currentBlock.moveLeft();
            redraw = true;
        }
    }

    private void moveRight() {
        if (gameBoard.checkRight(currentBlock))
        {
            currentBlock.moveRight();
            redraw = true;
        }
    }

    private void rotateLeft()
    {
        if(gameBoard.checkRotateLeft(currentBlock))
        {
            currentBlock.rotateLeft();
            redraw = true;
        }
        else
        {
            // See if the rotation would be valid if the block was tapped to the side (wall kick)
            currentBlock.moveRight();

            if(gameBoard.checkRotateLeft(currentBlock))
            {
                currentBlock.rotateLeft();
                redraw = true;
            }

            // Undo the move right if it still didn't work
            else
                currentBlock.moveLeft();
        }
    }

    private void rotateRight()
    {
        if(gameBoard.checkRotateRight(currentBlock))
        {
            currentBlock.rotateRight();
            redraw = true;
        }
        else
        {
            // See if the rotation would be valid if the block was tapped to the side (wall kick)
            currentBlock.moveLeft();

            if(gameBoard.checkRotateRight(currentBlock))
            {
                currentBlock.rotateRight();
                redraw = true;
            }
                // Undo the move left if it still didn't work
            else
                currentBlock.moveRight();
        }
    }
    // ===== End input handling =============================================

    // Checks for clears, clears them if so, and adds to the score and level
    private void checkClears()
    {
        int linesCleared = 0;

        // Get all the unique rows the block is spanning
        ArrayList<Integer> toCheck = new ArrayList<>();
        for(Coordinate c : currentBlock.getAbsoluteCoordinates())
        {
            if (!toCheck.contains(c.y))
                toCheck.add(c.y);
        }

        // Check each of those rows
        for(Integer i : toCheck)
        {
            if (gameBoard.checkLine(i))
            {
                // animations here later?
                gameBoard.clearLine(i);
                linesCleared++;
            }
        }

        if(linesCleared > 0)
        {
            combo += (linesCleared * 2) - 2;

            // Bonus for clearing the whole screen
            int bravo = gameBoard.equals(new Board()) ? 4 : 1;

            // Tetris: The Grand Master scoring method
            score += (Math.ceil((level + linesCleared) / 4) + droppedLines)
                    * linesCleared * ((linesCleared * 2) - 1)
                    * combo * bravo;
            level += linesCleared;

            // The game ends once the max level is reached.
            if(level >= maxLevel)
            {
                level = maxLevel;
                if(check3)
                {
                    // Final check. Score >= 126000, Time <= 13m30s
                    if(score < 126000 && System.currentTimeMillis() - startTimeMS > 810000)
                    {
                        grandmasterValid = false;
                    }
                    check3 = false;
                }
                gameOver(true);
            }
            redraw = true;
        }
        else
            combo = 1;
    }

    // Generates a block of a random type.
    private Block generateNewBlock()
    {
        switch((int) (Math.random() * 7)){
            case 0:
                return new BlockI(START);
            case 1:
                return new BlockJ(START);
            case 2:
                return new BlockL(START);
            case 3:
                return new BlockO(START);
            case 4:
                return new BlockS(START);
            case 5:
                return new BlockT(START);
            default:
                return new BlockZ(START);
        }
    }

    // Handling the game being won or lost
    private void gameOver(boolean win)
    {
        gameOver = true;
        if(!win)
            grandmasterValid = false;
    }

    private void addLevel(int toAdd)
    {
        level += toAdd;

        // Gravity changes depending on level
        if(level < 30)
            gravity = 32;
        else if(level < 35)
            gravity = 21;
        else if(level < 40)
            gravity = 16;
        else if(level < 50)
            gravity = 13;
        else if(level < 60)
            gravity = 10;
        else if(level < 70)
            gravity = 8;
        else if(level < 80)
            gravity = 4;
        else if(level < 140)
            gravity = 3;
        else if(level < 170)
            gravity = 2;
        else if(level < 200)
            gravity = 32;
        else if(level < 220)
            gravity = 4;
        else if(level < 230)
            gravity = 2;
        else if(level < 300)
            gravity = 1;
        else if(level < 330)
        {
            // Here, the pieces start dropping multiple rows per frame.
            gravity = 0;
            superGravity = 2;

            // This is one of the checkpoints for grandmaster rank. Score >= 12000, Time <= 4m15s
            if(check1)
            {
                if (score < 12000 || System.currentTimeMillis() - startTimeMS > 255000)
                {
                    grandmasterValid = false;
                    // No need to do the other checks if one fails.
                    check2 = false;
                    check3 = false;
                }
                check1 = false;
            }
        }
        else if(level < 360)
            superGravity = 3;
        else if(level < 400)
            superGravity = 4;
        else if(level < 420)
            superGravity = 5;
        else if(level < 450)
            superGravity = 4;
        else if(level < 500)
            superGravity = 3;
        else
        {
            superGravity = 20;
            // Another checkpoint. Score >= 40000 Time <= 7m30s
            if(check2)
            {
                if (score < 40000 || System.currentTimeMillis() - startTimeMS > 450000)
                {
                    grandmasterValid = false;
                    // No need to do the other checks if one fails.
                    check3 = false;
                }
                check2 = false;
            }
        }
    }

    public int[][] getStack() { return gameBoard.getStack(); }
    public Block getCurrentBlock() { return currentBlock; }
    public boolean getRedraw() { return redraw; }
    public void setRedraw(boolean redraw) { this.redraw = redraw; }

    // ===== Parcelable Stuff ============================================
    protected GameManager(Parcel in) {
        gameBoard = (Board) in.readValue(Board.class.getClassLoader());
        currentBlock = (Block) in.readValue(Block.class.getClassLoader());
        nextBlock = (Block) in.readValue(Block.class.getClassLoader());
        level = in.readInt();
        score = in.readInt();
        lockCurrent = in.readInt();
        droppedLines = in.readInt();
        combo = in.readInt();
        gameOver = in.readByte() != 0x00;
        spawnWait = in.readInt();
        fallWait = in.readInt();
        movementWait = in.readInt();
        startTimeMS = in.readLong();
        redraw = in.readByte() != 0x00;
        grandmasterValid = in.readByte() != 0x00;
        check1 = in.readByte() != 0x00;
        check2 = in.readByte() != 0x00;
        check3 = in.readByte() != 0x00;
        gravity = in.readInt();
        superGravity = in.readInt();
        maxLevel = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(gameBoard);
        dest.writeValue(currentBlock);
        dest.writeValue(nextBlock);
        dest.writeInt(level);
        dest.writeInt(score);
        dest.writeInt(lockCurrent);
        dest.writeInt(droppedLines);
        dest.writeInt(combo);
        dest.writeByte((byte) (gameOver ? 0x01 : 0x00));
        dest.writeInt(spawnWait);
        dest.writeInt(fallWait);
        dest.writeInt(movementWait);
        dest.writeLong(startTimeMS);
        dest.writeByte((byte) (redraw ? 0x01 : 0x00));
        dest.writeByte((byte) (grandmasterValid ? 0x01 : 0x00));
        dest.writeByte((byte) (check1 ? 0x01 : 0x00));
        dest.writeByte((byte) (check2 ? 0x01 : 0x00));
        dest.writeByte((byte) (check3 ? 0x01 : 0x00));
        dest.writeInt(gravity);
        dest.writeInt(superGravity);
        dest.writeInt(maxLevel);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<GameManager> CREATOR = new Parcelable.Creator<GameManager>() {
        @Override
        public GameManager createFromParcel(Parcel in) {
            return new GameManager(in);
        }

        @Override
        public GameManager[] newArray(int size) {
            return new GameManager[size];
        }
    };
}
