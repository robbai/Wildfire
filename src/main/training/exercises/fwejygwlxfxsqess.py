from dataclasses import dataclass, field
from typing import Any

from rlbot.utils.game_state_util import *
from rlbottraining.common_graders.goal_grader import *
from rlbottraining.training_exercise import TrainingExercise

@dataclass
class fwejygwlxfxsqess(TrainingExercise):
	grader: Any = PassOnGoalForAllyTeam(ally_team=0)

	def make_game_state(self,rng):
		return GameState(
			ball=BallState(physics=Physics(
				location=Vector3(3039.75,3704.3098,93.14),
				velocity=Vector3(-467.531,2375.661,0.0),
				angular_velocity=Vector3(-5.87211,-1.20911,0.23640999),
				rotation=Rotator(0.9466579,-0.2502306,3.1206923))),
			cars={
				0: CarState(
					physics=Physics(
						location=Vector3(1598.82,3722.8398,17.0),
						velocity=Vector3(585.79095,1029.721,0.36099997),
						angular_velocity=Vector3(-2.1E-4,-8.1E-4,2.63091),
						rotation=Rotator(-0.00958738,1.3444383,-9.58738E-5)),
					jumped=False,
					double_jumped=False,
					boost_amount=47.0),
				1: CarState(
					physics=Physics(
						location=Vector3(3139.75,2042.1499,17.06),
						velocity=Vector3(-954.22095,1097.891,0.141),
						angular_velocity=Vector3(-6.1E-4,8.1E-4,-0.097009994),
						rotation=Rotator(-0.016682042,2.2914796,9.58738E-5)),
					jumped=False,
					double_jumped=False,
					boost_amount=97.0)
			},
			boosts={i: BoostState(0) for i in range(34)},
		)
