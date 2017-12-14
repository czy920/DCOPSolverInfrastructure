package edu.cqu.core;

public interface ProgressChangedListener {
    void onProgressChanged(double percentage);
    void interrupted(String reason);
}
