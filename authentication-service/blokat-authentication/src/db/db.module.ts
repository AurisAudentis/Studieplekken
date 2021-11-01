import { Logger, Module } from '@nestjs/common';
import { ConfigModule } from '../configModule/config.module';
import { DbUserService } from './db-user/db-user.service';
import { DbService } from './db.service';

@Module({
  imports: [ConfigModule, Logger],
  providers: [DbUserService, DbService],
  exports: [DbUserService],
})
export class DbModule {}