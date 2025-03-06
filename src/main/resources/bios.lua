function pullEvent(sFilter)
    return coroutine.yield(sFilter)
end

-- Install globals
function sleep(nTime)
    if nTime ~= nil and type(nTime) ~= "number" then
        error("bad argument #1 (expected number, got " .. type(nTime) .. ")", 2)
    end
    local timer = startTimer(nTime or 0)
    repeat
        local sEvent, param = pullEvent("timer")
    until param == timer
end

-- pico = {}
function pico.move(side, length)
    pico.native.move(side, length or 1)
    pullEvent("pico_move")
end

function pico.turn(side)
    pico.native.turn(side)
    pullEvent("pico_turn")
end
