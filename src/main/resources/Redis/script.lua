local map = redis.call("HGETALL", KEYS[1])
local currSeg = tonumber(ARGV[1])
local segmentSize = tonumber(ARGV[2])
local windowSize = tonumber(ARGV[3])
local limit = tonumber(ARGV[4])
local sm = 0

for i = 1, #map, 2 do
	seg = tonumber(map[i])
	if seg < currSeg - (windowSize / segmentSize) then
		redis.call("HDEL", KEYS[1], seg)
	else
		sm = sm + tonumber(map[i + 1])
	end
end

if sm < limit then
	redis.call("HINCRBY", KEYS[1], currSeg, 1)
	return true
end

return false
