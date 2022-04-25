# Generated by Django 4.0.1 on 2022-04-18 02:26

from django.conf import settings
from django.db import migrations, models
import django.db.models.deletion
import django.utils.timezone


class Migration(migrations.Migration):

    initial = True

    dependencies = [
        migrations.swappable_dependency(settings.AUTH_USER_MODEL),
    ]

    operations = [
        migrations.CreateModel(
            name='catalog',
            fields=[
                ('id', models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('cate_name', models.CharField(max_length=100)),
            ],
        ),
        migrations.CreateModel(
            name='warehouse',
            fields=[
                ('id', models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('pos_x', models.IntegerField(default=0)),
                ('pos_y', models.IntegerField(default=0)),
            ],
        ),
        migrations.CreateModel(
            name='product',
            fields=[
                ('id', models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('product_name', models.CharField(max_length=100)),
                ('product_amt', models.IntegerField(default=0)),
                ('product_price', models.FloatField(default=0)),
                ('product_catalog', models.ForeignKey(null=True, on_delete=django.db.models.deletion.SET_NULL, to='shopping.catalog')),
            ],
        ),
        migrations.CreateModel(
            name='package_info',
            fields=[
                ('id', models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('dest_x', models.IntegerField()),
                ('dest_y', models.IntegerField()),
                ('package_job_time', models.DateTimeField(default=django.utils.timezone.now)),
                ('status', models.CharField(max_length=10)),
                ('ups_account', models.CharField(blank=True, max_length=100, null=True)),
                ('from_wh', models.ForeignKey(null=True, on_delete=django.db.models.deletion.SET_NULL, to='shopping.warehouse')),
                ('owner', models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, to=settings.AUTH_USER_MODEL)),
            ],
        ),
        migrations.CreateModel(
            name='order',
            fields=[
                ('id', models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('order_time', models.DateTimeField(default=django.utils.timezone.now)),
                ('product_amt', models.IntegerField(default=1)),
                ('owner', models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, to=settings.AUTH_USER_MODEL)),
                ('package_info', models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, to='shopping.package_info')),
                ('product', models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, to='shopping.product')),
            ],
        ),
    ]
