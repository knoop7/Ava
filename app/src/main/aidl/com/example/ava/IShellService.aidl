package com.example.ava;

interface IShellService {
    int executeCommand(String command) = 1;
    boolean setDisplayPower(int mode) = 2;
    void destroy() = 16777114;
}
