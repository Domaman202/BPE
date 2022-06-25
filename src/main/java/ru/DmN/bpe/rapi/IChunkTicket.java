package ru.DmN.bpe.rapi;

public interface IChunkTicket {
    void setTickCreated(long tickCreated);

    boolean isExpired(long currentTick);
}
