package com.osrs.client;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.ScreenAdapter;
import com.osrs.client.ui.LoginScreen;

/**
 * LibGDX Game wrapper.  Starts with LoginScreen; switches to GameScreen after auth.
 *
 * GameScreen extends ApplicationAdapter (not Screen), so we wrap it in a ScreenAdapter
 * that delegates lifecycle calls correctly.
 */
public class ErynfallGame extends Game {

    @Override
    public void create() {
        showLoginScreen();
    }

    /** Called by LoginScreen once credentials are confirmed. */
    public void startGame(String username, String password) {
        GameScreen gs = new GameScreen(this, username, password);
        setScreen(new ScreenAdapter() {
            @Override public void show()                       { gs.create(); }
            @Override public void render(float delta)         { gs.render(); }
            @Override public void resize(int w, int h)        { gs.resize(w, h); }
            @Override public void pause()                     { gs.pause(); }
            @Override public void resume()                    { gs.resume(); }
            @Override public void dispose()                   { gs.dispose(); }
        });
    }

    public void showLoginScreen() {
        showLoginScreen("");
    }

    public void showLoginScreen(String errorMessage) {
        setScreen(new LoginScreen(this, errorMessage));
    }
}
